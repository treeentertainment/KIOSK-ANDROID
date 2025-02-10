            const li = document.getElementById("check");
            let order = JSON.parse(localStorage.getItem('order')) || [];
            var shop = localStorage.getItem('name')?.replace(/"/g, '');
            const email = localStorage.getItem('email');

            // Appwrite SDK 초기화
            const client = new Appwrite.Client()
                .setEndpoint('https://cloud.appwrite.io/v1') // Replace with your Appwrite endpoint
                .setProject('treekiosk'); // Replace with your Appwrite project ID

            const database = new Appwrite.Databases(client);
            const account = new Appwrite.Account(client);

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
                const input = document.getElementById('numberDisplay').value; // 입력된 전화번호
                const databaseId = "tree-kiosk"; // 데이터베이스 ID
                const collectionIdOwner = "owner"; // 가게 정보 컬렉션 ID
                const collectionIdOrders = "data"; // 주문 데이터 컬렉션 ID
            
                try {
                    // 이메일로 해당 사용자의 문서 검색
                    const ownerDocuments = await database.listDocuments(
                        databaseId,
                        collectionIdOwner,
                        [Appwrite.Query.equal('email', email)] // 이메일 기준 필터링
                    );
            
                    if (ownerDocuments.total === 0) {
                        throw new Error("Owner document not found for the provided email.");
                    }
            
                    // 첫 번째 문서를 기준으로 진행
                    const ownerDocument = ownerDocuments.documents[0];
                    const currentOrderNumber = ownerDocument.order || 0;
            
                    // 주문 데이터 생성
                    const newOrder = {
                        shop: shop,
                        number: input,
                        ordernumber: currentOrderNumber.toString(), // 현재 주문 번호 사용
                        order: JSON.stringify(order), // 주문 데이터 JSON 문자열로 변환
                    };
            
                    const validDocumentId = currentOrderNumber.toString();
            
                    // Appwrite에 주문 추가
                    await database.createDocument(
                        databaseId,
                        collectionIdOrders,
                        validDocumentId,
                        newOrder
                    );
            
                    // 주문 데이터 제출 후 번호 증가
                    const newOrderNumber = parseInt(validDocumentId) + 1;
            
                    // 가게의 주문 번호 업데이트
                    await database.updateDocument(
                        databaseId,
                        collectionIdOwner,
                        ownerDocument.$id, // 이메일로 검색한 문서의 ID
                        { order: newOrderNumber.toString() } // 번호 증가
                    );
            
                    // 성공 후 처리
                    clearDisplay();
                    localStorage.removeItem("order");
            
                    alert("주문이 완료되었습니다.");
                    window.opener.postMessage("home", window.location.origin);
                    window.close();
                } catch (error) {
                    console.error("Error during submission:", error);
                    alert("Error during submission: " + error.message);
                }
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

    
            async function setLocal(email) {
                try {
                    const response = await database.listDocuments(
                    'tree-kiosk',        // 데이터베이스 ID
                    'owner',             // 컬렉션 ID
                        [Appwrite.Query.equal('email', email)] // 이메일 주소 사용
                    );
            
                    if (response.documents.length === 0) {
                        location.href = "index.html";
                        return;
                    }
            
                    const doc = response.documents[0]; // 첫 번째 검색 결과
                    if (doc.active) {
                        localStorage.setItem("name", doc.name);
                        localStorage.setItem("email", email);
                    } else {
                        location.href = "index.html";
                    }
                } catch (error) {
                    location.href = "index.html";
                }
            }
            
            
            document.addEventListener("DOMContentLoaded", function () {
                const email = localStorage.getItem('email');
                if (email) {
                setLocal(email);
                }
            });
