const form = document.getElementById("select");

// DOM 요소 참조
const ul = document.getElementById("list");
const myModalEl = document.getElementById('staticBackdrop');
const myModal = new bootstrap.Modal(myModalEl, { keyboard: false }); 

let order = JSON.parse(localStorage.getItem('order')) || []; // Load from localStorage or initialize

// 초기화 함수
window.addEventListener('load', init);

function init() {
    home();
    if (ul && ul.innerHTML.trim() === '') {
        loadItem();
    }
}

// 데이터 로드
function loadItem() {
             var data = window.AndroidApp.getJson(); 
            ul.innerHTML = ''; // Clear existing content
            for (let key in data) {
                const listItem = display(data[key], key);
                ul.appendChild(listItem);
            }
            // Preload images using lazy loading
            let images = document.querySelectorAll(".lazyload");
            fastLazyLoad(images);
        }

// 아이템 표시
function display(child, key) {
    const li = document.createElement('li');
    li.className = "list-group-item d-flex justify-content-between align-items-center item";

    const img = document.createElement('img');
    img.className = "lazyload";
    img.alt = child.name;
    img.dataset.id = key;
    img.src = child.placeholder || 'data:image/gif;base64,R0lGODlhAQABAIAAAAUEBA=='; // 작은 placeholder 이미지
    img.dataset.src = child.image;  // 실제 이미지는 나중에 로드
    img.dataset.srcset = `${child.imageSmall} 320w, ${child.image} 640w`; // 다양한 해상도 지원
    img.loading = 'eager';
    img.onclick = edit; // 편집 기능 연결
    const span = document.createElement('span');
    span.className = "info";
    span.textContent = child.name;

    li.appendChild(img);
    li.appendChild(span);

    return li;
}

// 최적화된 Lazy Load 함수
function fastLazyLoad(images) {
    const observer = new IntersectionObserver((entries, self) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                preloadImage(entry.target);
                self.unobserve(entry.target);
            }
        });
    }, { rootMargin: '0px 0px', threshold: 0.1 });

    images.forEach(image => observer.observe(image));
}

function preloadImage(img) {
    const src = img.dataset.src;
    if (src) {
        img.src = src; // 실제 이미지 로드
    }
    if (img.dataset.srcset) {
        img.srcset = img.dataset.srcset; // 고해상도 디스플레이 지원
    }
}

// 편집 기능
function edit(event) {
    var data = window.AndroidApp.getJson();
    const id = event.target.dataset.id;
            const item = data[id];

            // 기존 이벤트 리스너 제거 후 추가 (중복 방지)
            form.removeEventListener("submit", handleSubmit);
            form.removeEventListener("reset", resetForm);

            form.innerHTML = `
               <div class="modal-body">
                <img src="${item.image}" alt="${item.name}" data-id="${id}"><br>
                <span class="info">${item.name}</span><br><br>
                <div class="input-group flex-nowrap">
                    <button type="button" class="btn btn-outline-danger" onclick="decreaseQuantity()">-</button>
                    <input type="number" id="quantity" value="1" min="1" max="5" class="form-control"/>
                    <button type="button" class="btn btn-outline-success" onclick="increaseQuantity()">+</button>
                </div>
                <br>
                </div>
                      <div class="modal-footer">
                <button type="submit" class="btn btn-outline-success">확인</button>

                <button type="reset" class="btn btn-outline-warning">취소</button>
      </div>
            `;

            form.dataset.id = id;
            form.dataset.description = item.name;
            form.dataset.image = item.image;
            form.dataset.price = item.price;

            form.addEventListener("submit", handleSubmit);
            form.addEventListener("reset", resetForm);

            myModal.show();
        }

// 페이지 전환
function switchPage(pageId) {
    document.querySelectorAll('.page').forEach(page => {
        page.style.display = page.id === pageId ? 'block' : 'none';
    });
}

function home() {
    switchPage('main');
}

// 모달 닫기
function closemodal() {
    myModal.hide();
}

// 수량 감소
function decreaseQuantity() {
    const quantityInput = document.getElementById('quantity');
    let currentValue = parseInt(quantityInput.value);
    if (currentValue > 1) {
        quantityInput.value = currentValue - 1;
    }
}

// 수량 증가
function increaseQuantity() {
    const quantityInput = document.getElementById('quantity');
    let currentValue = parseInt(quantityInput.value);
    if (currentValue < 5) {
        quantityInput.value = currentValue + 1;
    }
}

// 폼 제출 처리
function handleSubmit(e) {
    e.preventDefault();

    const itemId = form.dataset.id;
    const description = form.dataset.description;
    const image = form.dataset.image;
    const price = parseFloat(form.dataset.price);
    const quantity = parseInt(document.getElementById("quantity").value);

    const urlParams = { id: itemId, description, image, price, quantity };
    addItemToOrder(urlParams);

    closemodal();
}

// 폼 리셋
function resetForm(event) {
    event.preventDefault();
    document.getElementById("quantity").value = 1;
    closemodal();
}

// 주문 항목 추가
function addItemToOrder({ id, image, description, price, quantity }) {
    const existingIndex = getItemIndex(description);

    if (existingIndex !== -1) {
        order[existingIndex].quantity += quantity;
        order[existingIndex].price += quantity * (price / quantity);
    } else {
        order.push({ id, image, description, quantity, price });
    }
    saveOrder();
}

function saveOrder() {
    localStorage.setItem('order', JSON.stringify(order));
}
// 아이템 인덱스 찾기
function getItemIndex(description) {
    return order.findIndex(item => item.description.trim() === description.trim());
}



// 초기 로그인 확인
function openWindow(name) {

    var url = `${name}`;

    var win = window.open(url, '_blank');
    win.focus();
  }
  
  window.addEventListener("message", function (event) {
  if (event.origin !== window.location.origin) {
    return;
  }
  if (event.data === "home") {
    window.location.href = `index.html`;
  }
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
