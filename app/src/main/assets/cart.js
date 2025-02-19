const cart = document.getElementById("cart-list");

function getOrder() {
    return JSON.parse(localStorage.getItem('order')) || [];
}

const cartshow = debounce(function () {
    renderCart();
}, 300);

window.addEventListener('load', cartshow);

function renderCart() {
    let order = getOrder();
    cart.innerHTML = order.length === 0 ? `
        <li class="list-group-item d-flex justify-content-between align-items-center">
            <div>장바구니가 비어있습니다.</div>
        </li>` : order.map((item, index) => `
        <li class="list-group-item d-flex justify-content-between align-items-center item cart">
            <img src="${item.image}" alt="${item.description}">
            <span class="info">${item.description}</span>
            <div class="input-group mb-3">
                <input type="number" min="1" value="${item.quantity}" id="${index}" class="form-control" placeholder="quantity">
                <button type="button" class="btn btn-outline-danger" onclick="deleteItem(${index})">삭제</button>
            </div>
        </li>
    `).join('');

    order.forEach((item, index) => {
        document.getElementById(index).addEventListener("change", debounce(event => {
            updateItemQuantity(item.description, parseInt(event.target.value));
        }, 300));
    });
}

function updateItemQuantity(description, newQuantity) {
    const itemIndex = getItemIndex(description);
    if (itemIndex !== -1 && newQuantity > 0) {
        const originalPrice = order[itemIndex].price / order[itemIndex].quantity;
        order[itemIndex].quantity = newQuantity;
        order[itemIndex].price = newQuantity * originalPrice;
        saveOrder(); // Save order to localStorage
        renderCart(); // 카트 화면을 새로 고침
    }
}

function saveOrder() {
    let order = getOrder();
    localStorage.setItem('order', JSON.stringify(order));
}

function deleteItem(index) {
    let order = getOrder();
    order.splice(index, 1);
    saveOrder(); // Save order to localStorage after deletion
    cartshow();
    console.log('Deleted Item:', order); // Debugging: Log current order
}

function addItemToOrder({ id, image, description, price, quantity }) {
    let order = getOrder();
    const existingIndex = getItemIndex(description);
    if (existingIndex !== -1) {
        order[existingIndex].quantity += quantity;
        order[existingIndex].price += quantity * (price / quantity);
    } else {
        order.push({ id, image, description, quantity, price });
    }
   saveOrder(); 
}

function getItemIndex(description) {
    let order = getOrder();
    return order.findIndex(item => item.description === description);
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
  
  function onLoginFailure(message) {
    
      window.location.href = "index.html";

  }

     function onUserExists(exists, email, name) {
            localStorage.setItem("name", name);
            localStorage.setItem("email", email);
            if (exists === false) {
                window.location.href = "index.html";
             }
        }

window.addEventListener('load', function() {   
    window.AndroidApp.checkUserDocument(localStorage.getItem('email'));
});
