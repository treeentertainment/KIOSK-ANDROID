 // AndroidBridge를 통해 로그인 요청
  function loginpassword() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    if (window.AndroidBridge && typeof window.AndroidBridge.signInWithEmailAndPassword === 'function') {
      window.AndroidBridge.signInWithEmailAndPassword(email, password);
    } else {
      alert("AndroidBridge를 사용할 수 없습니다.");
    }

    console.log("로그인 시도:", email, password);

  }

  // 로그인 상태 확인 요청
  function checkAuthState() {
    if (window.AndroidBridge && typeof window.AndroidBridge.checkAuthState === 'function') {
      window.AndroidBridge.checkAuthState();
    }
  }

  // 페이지 로딩 시 로그인 상태 확인
  window.onload = checkAuthState;

  // Enter 키 입력 시 로그인 시도
  document.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
      loginpassword();
    }
  });

  // Android에서 전달받은 정보를 바탕으로 UI 전환
  function show(shown, hidden) {
    document.getElementById(shown).style.display = 'block';
    document.getElementById(hidden).style.display = 'none';
    return false;
  }

  // 로그아웃 처리
  async function logout() {
    try {
      window.AndroidBridge.logOut();

      localStorage.clear();
      sessionStorage.clear();
      clickCount = 0;

      window.location.reload(); // 강제로 새로고침
    } catch (error) {
      console.error('Error during logout:', error);
    }
  }
  // 로그아웃 트리거 (5번 클릭 시)
  let clickCount = 0;
  document.getElementById('logout-link').addEventListener('click', async (e) => {
    e.preventDefault();
    clickCount++;
    if (clickCount === 5) {
      await logout();
    }
  });

  // 전역 함수로 노출
  window.loginpassword = loginpassword;
  window.checkAuthState = checkAuthState;
  window.logout = logout;
  window.show = show;

  function loginfinish() {
  show('startface', 'login-container');
  }

  function loginfail(message) {
  if(message != null) {
       alert(message);
  }
    show('login-container', 'startface');
   }


function closeModal() {
    console.log(window.moveable);
    if (window.moveable) {
        document.getElementById("alertbox").classList.remove("active");
    }
}
