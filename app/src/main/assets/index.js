       let clickCount = 0;

        // 특정 요소를 보여주거나 숨기는 함수
        function toggleVisibility(elementsToShow, elementsToHide) {
            elementsToShow.forEach(id => document.getElementById(id).style.display = 'block');
            elementsToHide.forEach(id => document.getElementById(id).style.display = 'none');
        }

        // Android 네이티브 코드에서 호출하는 함수 (회원 여부 확인 후 UI 업데이트)
        function handleAuthResult(success, member) {
            if (success) {
                if (member) {
                    toggleVisibility(['front'], ['login-container']);
                } else {
                    toggleVisibility(['nouser'], ['front', 'login-container']);
                }
            } else {
                console.error('로그인 실패');
                toggleVisibility(['login-container'], ['front']);
            }
        }

        // Google 로그인 버튼 클릭 시 AndroidInterface 호출
        document.getElementById('googleLoginBtn').addEventListener('click', function() {
            login();
        });

        document.getElementById('logout-link').addEventListener('click', async (e) => {
            e.preventDefault();
            clickCount++;

            if (clickCount === 5) { // 5번 클릭 시 로그아웃 실행
                try {
               logout();
                } catch (error) {
                    console.error('로그아웃 중 오류 발생:', error);
                }
            }
        });

        // 페이지 로드 시 Android에서 로그인 상태 확인 요청
        document.addEventListener('DOMContentLoaded', () => {
            if (window.AndroidApp) {
                window.AndroidApp.checkAuthState();
            }
        })

        function login() {
            window.AndroidApp.googleLogin();
        }

        function onLoginSuccess(email) {
            window.AndroidApp.checkUserDocument(email);
        }

        function onLoginFailure(message) {
          toggleVisibility(['login-container'], ['front']);
        }

        function logout() {
            window.AndroidApp.logout();
        }

        function onLogoutSuccess() {
            toggleVisibility(['login-container'], ['front']);    
        }


        function onUserExists(exists) {
            if (exists !== false) {
            toggleVisibility(['front'], ['login-container']);
             } else {
            toggleVisibility(['nouser'], ['front', 'login-container']);
           }
       }

