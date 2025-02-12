            const li = document.getElementById("check");
            let order = JSON.parse(localStorage.getItem('order')) || [];
            
            function renderCheckout() {
                li.innerHTML = '';
                if (order.length === 0) {
                    li.innerHTML = `<li class="list-group-item d-flex justify-content-between align-items-center">
                        <div>장바구니가 비어있습니다.</div>
                    </li>`;
                } else {
                    order.forEach((item) => {
                        li.innerHTML += `
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                <img src="${item.image}" alt="${item.description}" class="item-img">
                                <div>
                                    <span>${item.description}</span>
                                    <span>Quantity: ${item.quantity}</span>
                                    <span>Price: ${item.price}</span>
                                </div>
                            </li>
                        `;
                    });
                }
            }

            window.addEventListener('load', renderCheckout);

            function appendNumber(num) {
                var input = document.getElementById('numberDisplay');
                if (input.value.length < 13) {
                    input.value = formatPhoneNumber(input.value + num);
                }
                toggleSendButton();
            }

            function clearDisplay() {
                var input = document.getElementById('numberDisplay');
                input.value = '010-';
                toggleSendButton();
            }

            function backspace() {
                var input = document.getElementById('numberDisplay');
                if (input.value.length > 4) {
                    input.value = formatPhoneNumber(input.value.slice(0, -1));
                }
                toggleSendButton();
            }

            function toggleSendButton() {
                var input = document.getElementById('numberDisplay');
                var sendButton = document.getElementById('sendButton');
                sendButton.style.display = (input.value.length === 13) ? 'block' : 'none';
            }

            async function submit() {
            const orderData = JSON.stringify(localStorage.getItem('order'));
             var shop = localStorage.getItem('name')?.replace(/"/g, '');
              const email = localStorage.getItem('email');
              const phoneNumber = document.getElementById('numberDisplay').value; 
              window.AndroidApp.submitOrder(phoneNumber, email, shop, orderData);
            }


            function formatPhoneNumber(value) {
                value = value.replace(/[^0-9]/g, '');
                if (value.length > 3) {
                    value = value.slice(0, 3) + '-' + value.slice(3);
                }
                if (value.length > 8) {
                    value = value.slice(0, 8) + '-' + value.slice(8, 12);
                }
                return value;
            }

            function cert() {
                const pages = document.querySelectorAll('.page');
                pages.forEach(page => {
                    page.style.display = page.id === "certificate" ? 'block' : 'none';
                
                });
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

function finishsend() {
   clearDisplay();
   localStorage.removeItem("order");
   alert("주문이 완료되었습니다.");
   window.AndroidApp.sendMessage("home");   
}

function errorsend(message) {
    alert(message);
    alert("다시 시도 하십시오");
}
