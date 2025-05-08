var sendButton = document.getElementById('sendButton');
var input = document.getElementById('numberDisplay');
var email = JSON.parse(window.localStorage.getItem('email'));
var number = JSON.parse(window.localStorage.getItem('number'));
let timer; // 전역 변수로 선언

window.onload = checkAuthState;

function checkAuthState() {
      const div = document.querySelector('.keypad'); // 또는 적절한 div 선택자
      const buttons = div.querySelectorAll('button');

      buttons.forEach(button => {
        button.disabled = true;
      });

    if (window.AndroidBridge && typeof window.AndroidBridge.checkAuthState === 'function') {
      window.AndroidBridge.checkAuthState();
    } else {
       const message = JSON.stringify({ type: "noselect"});
       window.AndroidBridge.postMessage(message);
    }
  }

function loginfinish() {
      const div = document.querySelector('.keypad'); // 또는 적절한 div 선택자
      const buttons = div.querySelectorAll('button');

      buttons.forEach(button => {
        button.disabled = false;
      });
 }

function loginfail() {
   const message = JSON.stringify({ type: "noselect"});
   window.AndroidBridge.postMessage(message);
}

function getOrder() {
  return JSON.parse(localStorage.getItem('order')) || [];
  }

  function appendNumber(num) {
    if (input.value === '010-') {
        // '010-' 뒤에 숫자를 추가
        input.value = `010-${num}`;
    } else if (input.value.length < 13) {
        input.value = formatPhoneNumber(input.value + num);
    }
    toggleSendButton();
}

  function clearDisplay() {
    input.value = '010-';
    toggleSendButton();
  }

  function backspace() {
    if (input.value.length > 4) {
        let newValue = input.value.slice(0, -1);

        // 010 다음에 '-'가 자동으로 추가되도록 보정
        if (newValue === '010' || newValue === '010-') {
            input.value = '010-';
        } 
        // '-'로 끝나면 한 글자 더 삭제
        else if (newValue.endsWith('-')) {
            input.value = newValue.slice(0, -1);
        } 
        else {
            input.value = newValue;
        }
    }
    toggleSendButton();
}



  function toggleSendButton() {
    sendButton.style.display = (input.value.length === 13) ? 'block' : 'none';
  }

  function formatPhoneNumber(value) {
    value = value.replace(/[^0-9]/g, '');
    if (value.length > 4) {
        value = value.slice(0, 3) + '-' + value.slice(3);
    }
    if (value.length > 8) {
        value = value.slice(0, 8) + '-' + value.slice(8, 12);
    }
    return value;
  }

document.addEventListener("keydown", (event) => {
  if (event.key >= "0" && event.key <= "9") {
      document.getElementsByClassName(event.key)[0].click();  // 숫자 버튼 클릭
  } else if (event.key === "Enter") {  // 엔터를 누르면 제출출
      if(sendButton.style.display === 'block') {
          sendButton.click();
      }
  } else if (event.key === "Backspace") {  // 마지막 숫자 삭제
    document.getElementsByClassName(event.key)[0].click(); 
  }
});


document.getElementById('sendButton').addEventListener('click', function() {
  const div = document.querySelector('.keypad');
  const buttons = div.querySelectorAll('button');
  buttons.forEach(button => button.disabled = true);

  const order = getOrder();
  const numberDisplayValue = document.getElementById('numberDisplay').value;

  console.log(order);
  AndroidBridge.sendOrder(JSON.stringify(order), number, numberDisplayValue);
  });


  function sendfinish() {
    document.getElementById("alertbox").classList.add("active");

    let seconds = 10;
    const countdownEl = document.getElementById('countdown');

    timer = setInterval(() => {
      seconds--;
      countdownEl.textContent = `${seconds}초`;

      if (seconds <= 0) {
        clearInterval(timer);
        document.getElementById("alertbox").classList.remove("active");
         const message = JSON.stringify({ type: "original"});
         window.AndroidBridge.postMessage(message);
      }
    }, 1000);
  }

  function gocancel() {
    clearInterval(timer); // 타이머 멈춤
    document.getElementById("alertbox").classList.remove("active");
    const message = JSON.stringify({ type: "original"});
    window.AndroidBridge.postMessage(message);
  }
