const API_BASE = '/api';
let currentCustomerId = null;

// При загрузке страницы
document.addEventListener('DOMContentLoaded', async () => {
    await initCustomer();
    await loadCatalog(); // Загружаем модели
    await loadCart();    // Загружаем корзину
});

// 1. Инициализация клиента
async function initCustomer() {
    try {
        // 1. Сначала проверяем, есть ли уже клиенты в базе
        const checkResponse = await fetch(`${API_BASE}/customers`, { cache: 'no-store' });
        const customers = await checkResponse.json();

        if (customers && customers.length > 0) {
            // Если клиент есть - берем первого
            const existingCustomer = customers[0];
            currentCustomerId = existingCustomer.id;
            document.getElementById('user-name-display').textContent = existingCustomer.name;

            // Берем его РЕАЛЬНЫЙ баланс из базы
            updateBalanceDisplay(existingCustomer.balance);
        } else {
            // 2. Если клиентов нет - создаем нового
            const response = await fetch(`${API_BASE}/customers`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: "Иван Иванов",
                    email: "ivan@test.com"
                })
            });

            if (response.ok) {
                const customer = await response.json();
                currentCustomerId = customer.id;
                document.getElementById('user-name-display').textContent = customer.name;
                updateBalanceDisplay(customer.balance);
            }
        }
    } catch (e) {
        console.error("Ошибка инициализации клиента", e);
    }
}

// 2. ЗАГРУЗКА КАТАЛОГА (ЭТОЙ ФУНКЦИИ У ВАС НЕ БЫЛО)
async function loadCatalog() {
    const container = document.getElementById('models-container');
    container.innerHTML = '<p>Загрузка...</p>';

    try {
        let response = await fetch(`${API_BASE}/packages`, { cache: 'no-store' });
        let packages = await response.json();

        if (packages.length === 0) {
            await seedData();
            const newResponse = await fetch(`${API_BASE}/packages`, { cache: 'no-store' });
            packages = await newResponse.json();
        }

        container.innerHTML = '';

        packages.forEach(pkg => {
            // ПРОВЕРЯЕМ ОСТАТОК
            const isOutOfStock = pkg.stock <= 0;

            // Если товара нет, делаем кнопку неактивной и меняем текст
            const buttonHtml = isOutOfStock
                ? `<button class="btn-primary" disabled style="background-color: #9ca3af; cursor: not-allowed;">Нет в наличии</button>`
                : `<button class="btn-primary" onclick="addToCart(${pkg.id})">Добавить в корзину</button>`;

            const card = document.createElement('div');
            card.className = 'card';
            card.innerHTML = `
                <div class="card-header">
                    <div class="card-title">${pkg.model.name}</div>
                    <div class="card-provider">${pkg.model.provider}</div>
                </div>
                <div class="card-body">
                    <p>Пакет токенов: ${pkg.tokenAmount.toLocaleString()} шт.</p>
                    <p style="color: ${isOutOfStock ? 'red' : 'inherit'};">
                        В наличии: ${pkg.stock} шт.
                    </p>
                </div>
                <div class="card-price">${pkg.price} ₽</div>
                ${buttonHtml}
            `;
            container.appendChild(card);
        });
    } catch (e) {
        console.error(e);
        container.innerHTML = '<p>Ошибка загрузки каталога. Проверьте консоль (F12).</p>';
    }
}

// 3. Создание тестовых данных (Seed)
async function seedData() {
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
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenAmount: 1000000, price: 500, stock: 10, model: { id: model1.id } })
    });

    await fetch(`${API_BASE}/packages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenAmount: 5000000, price: 2000, stock: 5, model: { id: model2.id } })
    });
}

// 4. Добавление в корзину
async function addToCart(packageId) {
    if (!currentCustomerId) return alert("Клиент еще не создан!");

    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/cart/items?packageId=${packageId}&quantity=1`, {
            method: 'POST'
        });

        if (response.ok) {
            await loadCart();
        } else {
            const err = await response.json();
            alert(`Ошибка: ${err.message}`);
        }
    } catch (e) {
        alert("Ошибка добавления в корзину");
    }
}

// 5. Загрузка и отображение корзины СО СТРЕЛОЧКАМИ
async function loadCart() {
    if (!currentCustomerId) return;

    const container = document.getElementById('cart-container');
    const totalContainer = document.getElementById('cart-total-container');

    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/cart`);
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
                            <strong>${item.tokenPackage.model.name}</strong><br>
                            <small>${item.tokenPackage.price} ₽ / шт.</small>
                        </div>
                        <div class="cart-item-controls">
                            <button class="qty-btn" onclick="changeQuantity(${item.tokenPackage.id}, ${item.quantity - 1})">-</button>
                            <span class="qty-value">${item.quantity}</span>
                            <button class="qty-btn" onclick="changeQuantity(${item.tokenPackage.id}, ${item.quantity + 1})">+</button>
                        </div>
                        <div>${itemTotal} ₽</div>
                    </div>
                `;
            });

            document.getElementById('cart-total').textContent = total;
            totalContainer.style.display = 'block';
        } else {
            container.innerHTML = '<p class="empty-cart">Корзина пока пуста</p>';
            totalContainer.style.display = 'none';
        }
    } catch (e) {
        console.error("Ошибка загрузки корзины", e);
    }
}

// 6. Изменение количества (стрелочки)
async function changeQuantity(packageId, newQuantity) {
    if (!currentCustomerId) return;

    try {
        const response = await fetch(`${API_BASE}/customers/${currentCustomerId}/cart/items/${packageId}?quantity=${newQuantity}`, {
            method: 'PUT'
        });

        if (response.ok) {
            await loadCart();
        } else {
            const err = await response.json();
            alert(`Ошибка: ${err.message}`);
        }
    } catch (e) {
        alert("Ошибка изменения количества");
    }
}

// 7. Обновление баланса
function updateBalanceDisplay(newBalance) {
    document.getElementById('balance-amount').textContent = newBalance.toFixed(2);
}

// 8. Оформление заказа
document.getElementById('checkout-btn').addEventListener('click', async () => {
    try {
        const response = await fetch(`${API_BASE}/orders?customerId=${currentCustomerId}`, {
            method: 'POST'
        });

        if (response.ok) {
            const order = await response.json();
            alert(`Заказ успешно оформлен! Сумма: ${order.totalPrice} ₽`);

            await loadCart();
            await loadCatalog(); // Обновляем остатки на складе

            const custResponse = await fetch(`${API_BASE}/customers/${currentCustomerId}`);
            const updatedCustomer = await custResponse.json();
            updateBalanceDisplay(updatedCustomer.balance);

        } else {
            const err = await response.json();
            alert(`Ошибка оформления: ${err.message || 'Неизвестная ошибка'}`);
        }
    } catch (e) {
        alert(`Ошибка: ${e.message}`);
    }
});