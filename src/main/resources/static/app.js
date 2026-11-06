const API_BASE = '/api';
let currentCustomerId = null;

document.addEventListener('DOMContentLoaded', async () => {
    await initCustomer();
    await loadCatalog();
    await loadCart();
    await loadOrders();
});

async function initCustomer() {
    try {
        const checkResponse = await fetch(`${API_BASE}/customers`, { cache: 'no-store' });
        if (!checkResponse.ok) return;
        const customers = await checkResponse.json();

        if (customers && customers.length > 0) {
            const existingCustomer = customers[0];
            currentCustomerId = existingCustomer.id;
            document.getElementById('user-name-display').textContent = existingCustomer.name || "Клиент";
            updateBalanceDisplay(existingCustomer.balance || 0);
        } else {
            const response = await fetch(`${API_BASE}/customers`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: "Иван Иванов", email: "ivan@test.com" })
            });
            if (response.ok) {
                const customer = await response.json();
                currentCustomerId = customer.id;
                document.getElementById('user-name-display').textContent = customer.name;
                updateBalanceDisplay(customer.balance || 0);
            }
        }
    } catch (e) { console.error("Ошибка инициализации", e); }
}

async function loadCatalog() {
    const container = document.getElementById('models-container');
    container.innerHTML = '<p>Загрузка...</p>';
    try {
        let response = await fetch(`${API_BASE}/packages`, { cache: 'no-store' });
        if (!response.ok) return;
        let packages = await response.json();
        if (packages.length === 0) {
            await seedData();
            const newResponse = await fetch(`${API_BASE}/packages`, { cache: 'no-store' });
            packages = await newResponse.json();
        }
        renderPackages(packages);
    } catch (e) { console.error(e); }
}

function renderPackages(packages) {
    const container = document.getElementById('models-container');
    container.innerHTML = '';
    if (!packages || packages.length === 0) {
        container.innerHTML = '<p style="color:gray">Ничего не найдено</p>';
        return;
    }

    packages.forEach(pkg => {
        const isOutOfStock = pkg.stock <= 0;
        const buttonHtml = isOutOfStock
            ? `<button class="btn-primary" disabled>Нет в наличии</button>`
            : `<button class="btn-primary" onclick="addToCart(${pkg.id})">Добавить в корзину</button>`;

        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
            <div class="card-header">
                <div class="card-title">${pkg.model ? pkg.model.name : 'Модель'}</div>
                <div class="card-provider">${pkg.model ? pkg.model.provider : 'N/A'}</div>
            </div>
            <div class="card-body">
                <p>Пакет токенов: ${pkg.tokenAmount ? pkg.tokenAmount.toLocaleString() : 0} шт.</p>
                <p style="color: ${isOutOfStock ? '#ef4444' : 'inherit'}; margin-top: 5px;">
                    В наличии: ${pkg.stock} шт.
                </p>
            </div>
            <div class="card-price">${pkg.price} ₽</div>
            ${buttonHtml}
        `;
        container.appendChild(card);
    });
}

async function seedData() {
    try {
        let res = await fetch(`${API_BASE}/models`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: "GPT-4 Turbo", provider: "OPENAI", pricePer1000Tokens: 0.03, contextWindow: 128000 })
        });
        const model1 = await res.json();
        res = await fetch(`${API_BASE}/models`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: "Claude 3 Opus", provider: "ANTHROPIC", pricePer1000Tokens: 0.05, contextWindow: 200000 })
        });
        const model2 = await res.json();
        await fetch(`${API_BASE}/packages`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tokenAmount: 1000000, price: 500, stock: 10, model: { id: model1.id } })
        });
        await fetch(`${API_BASE}/packages`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tokenAmount: 5000000, price: 2000, stock: 5, model: { id: model2.id } })
        });
    } catch (e) { console.error("Ошибка seed", e); }
}

async function addToCart(packageId) {
    if (!currentCustomerId) return alert("Клиент не создан!");
    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/cart/items?packageId=${packageId}&quantity=1`, { method: 'POST' });
        if (response.ok) await loadCart();
        else alert(`Ошибка: ${(await response.json()).message}`);
    } catch (e) { alert("Ошибка добавления"); }
}

async function loadCart() {
    if (!currentCustomerId) return;
    const container = document.getElementById('cart-container');
    const totalContainer = document.getElementById('cart-total-container');

    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/cart`);
        if (!response.ok) return;
        const cart = await response.json();

        if (cart.items && cart.items.length > 0) {
            container.innerHTML = '';
            let total = 0;
            cart.items.forEach(item => {
                const itemTotal = item.tokenPackage.price * item.quantity;
                total += itemTotal;
                container.innerHTML += `
                    <div class="cart-item">
                        <div>
                            <div class="cart-item-title">${item.tokenPackage.model.name}</div>
                            <div class="cart-item-price">${item.tokenPackage.price} ₽ / шт.</div>
                        </div>
                        <div class="cart-item-controls">
                            <button class="qty-btn" onclick="changeQuantity(${item.tokenPackage.id}, ${item.quantity - 1})">−</button>
                            <span class="qty-value">${item.quantity}</span>
                            <button class="qty-btn" onclick="changeQuantity(${item.tokenPackage.id}, ${item.quantity + 1})">+</button>
                        </div>
                        <div style="font-weight: 600; min-width: 60px; text-align: right;">${itemTotal} ₽</div>
                    </div>
                `;
            });
            document.getElementById('cart-total').textContent = total;
            totalContainer.style.display = 'block';
        } else {
            container.innerHTML = '<p class="empty-state">Ваша корзина пуста</p>';
            totalContainer.style.display = 'none';
        }
    } catch (e) { console.error(e); }
}

async function changeQuantity(packageId, newQuantity) {
    if (!currentCustomerId) return;
    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/cart/items/${packageId}?quantity=${newQuantity}`, { method: 'PUT' });
        if (response.ok) await loadCart();
        else alert(`Ошибка: ${(await response.json()).message}`);
    } catch (e) { alert("Ошибка изменения"); }
}

function updateBalanceDisplay(newBalance) {
    document.getElementById('balance-amount').textContent = Number(newBalance).toFixed(2);
}

document.getElementById('checkout-btn').addEventListener('click', async () => {
    try {
        const response = await fetch(`${API_BASE}/orders?customerId=${currentCustomerId}`, { method: 'POST' });
        if (response.ok) {
            await loadCart();
            await loadCatalog();
            await loadOrders();
            const custResponse = await fetch(`${API_BASE}/customers/${currentCustomerId}`);
            const updatedCustomer = await custResponse.json();
            updateBalanceDisplay(updatedCustomer.balance);
        } else {
            alert(`Ошибка: ${(await response.json()).message}`);
        }
    } catch (e) { alert(`Ошибка: ${e.message}`); }
});

async function loadOrders() {
    if (!currentCustomerId) return;
    const container = document.getElementById('orders-container');
    const clearBtn = document.getElementById('clear-history-btn');

    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/orders`, { cache: 'no-store' });
        if (!response.ok) return;
        const orders = await response.json();

        if (!orders || orders.length === 0) {
            container.innerHTML = '<p class="empty-state">Заказов пока нет</p>';
            clearBtn.style.display = 'none';
            return;
        }

        clearBtn.style.display = 'block';
        container.innerHTML = '';
        orders.sort((a, b) => b.id - a.id);

        orders.forEach(order => {
            const items = order.items || [];
            const itemsHtml = items.map(item =>
                `<div>• ${item.modelName} (${item.tokenAmount ? item.tokenAmount.toLocaleString() : 0} токенов) x${item.quantity} шт. — ${item.price * item.quantity} ₽</div>`
            ).join('');

            const finalItemsHtml = itemsHtml || '<div style="color:gray">Нет данных о товарах</div>';
            const isCancellable = order.status !== 'COMPLETED' && order.status !== 'CANCELLED';
            const cancelBtnHtml = isCancellable
                ? `<button class="btn-cancel" onclick="cancelOrder(${order.id})">Отменить заказ</button>`
                : `<button class="btn-cancel" disabled>Отмена недоступна</button>`;

            const date = new Date(order.orderDate).toLocaleString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });

            const orderCard = document.createElement('div');
            orderCard.className = 'order-card';
            orderCard.innerHTML = `
                <div class="order-header">
                    <div>
                        <span class="order-id">Заказ #${order.id}</span>
                        <span class="order-date">${date}</span>
                    </div>
                    <span class="status-badge status-${order.status}">${order.status}</span>
                </div>
                <div class="order-items-list">${finalItemsHtml}</div>
                <div class="order-footer">
                    <span class="order-total">Итого: ${order.totalPrice} ₽</span>
                    ${cancelBtnHtml}
                </div>
            `;
            container.appendChild(orderCard);
        });
    } catch (e) { console.error(e); }
}

async function cancelOrder(orderId) {
    if (!confirm("Вы уверены, что хотите отменить этот заказ?")) return;
    try {
        const response = await fetch(`${API_BASE}/orders/${orderId}/status?status=CANCELLED`, { method: 'PATCH' });
        if (response.ok) {
            await loadOrders();
            await loadCatalog();
            const custResponse = await fetch(`${API_BASE}/customers/${currentCustomerId}`);
            const updatedCustomer = await custResponse.json();
            updateBalanceDisplay(updatedCustomer.balance);
        } else {
            alert(`Ошибка: ${(await response.json()).message}`);
        }
    } catch (e) { alert("Ошибка сети"); }
}

// НОВАЯ ФУНКЦИЯ ДЛЯ ОЧИСТКИ ИСТОРИИ
async function clearHistory() {
    if (!confirm("Вы уверены, что хотите удалить всю историю заказов? Это действие нельзя отменить.")) return;

    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/orders`, {
            method: 'DELETE'
        });

        if (response.ok || response.status === 204) {
            await loadOrders(); // Обновляем список (он станет пустым)
        } else {
            alert("Не удалось очистить историю");
        }
    } catch (e) {
        alert("Ошибка сети при очистке истории");
    }
}

// Поиск и сортировка
let searchTimeout;
document.getElementById('search-input').addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        loadCatalogWithParams(e.target.value, document.getElementById('sort-select').value);
    }, 300);
});

document.getElementById('sort-select').addEventListener('change', (e) => {
    loadCatalogWithParams(document.getElementById('search-input').value, e.target.value);
});

async function loadCatalogWithParams(modelName, sortBy) {
    const container = document.getElementById('models-container');
    container.innerHTML = '<p>Загрузка...</p>';
    try {
        let url = `${API_BASE}/packages?sortBy=${sortBy}`;
        if (modelName) url += `&modelName=${encodeURIComponent(modelName)}`;
        const response = await fetch(url, { cache: 'no-store' });
        let packages = await response.json();
        renderPackages(packages);
    } catch (e) { console.error(e); }
}