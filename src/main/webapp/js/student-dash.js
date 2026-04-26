let registerMsgTimeout = null;
let dropMsgTimeout = null;


function showMessage(div, text, color, timeoutVarName) {
    div.textContent = text;
    div.style.color = color;
    if (window[timeoutVarName]) clearTimeout(window[timeoutVarName]);
    window[timeoutVarName] = setTimeout(() => {
        div.textContent = "";
    }, 3000);
}

document.addEventListener("DOMContentLoaded", function () {
    
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    document.querySelector('.tab[data-tab="catalog"]').classList.add('active');
    document.getElementById("catalog").classList.add('active');
    
    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', function () {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
            this.classList.add('active');
            document.getElementById(this.dataset.tab).classList.add('active');
           
            document.getElementById("register-message").textContent = "";
            document.getElementById("drop-message").textContent = "";
            switch (this.dataset.tab) {
                case "catalog": fetchCourses(); break;
                case "enrolled": fetchEnrolledCourses(); break;
                case "results": fetchResults(); break;
                case "drop": fetchCoursesForDrop(); break;
            }
        });
    });

    
    fetchStudentName();
    fetchCourses();

    
    document.getElementById("register-form").addEventListener("submit", function (e) {
        e.preventDefault();
        registerCourseByCode();
    });

    
    document.getElementById("catalog-search-btn").addEventListener("click", function () {
        fetchCourses();
    });

    document.getElementById("logout-btn").addEventListener("click", function () {
        fetch("/course-portal/logout", { method: "POST" })
            .then(() => {
                window.location.href = "index.html";
            });
    });
});


function fetchStudentName() {
    fetch("/course-portal/StudentDashServlet?action=session")
        .then(res => res.json())
        .then(data => {
            if (data && data.name) {
                document.getElementById("student-name").textContent = "Welcome, " + data.name;
            }
        });
}


function fetchCourses() {
    const department = document.getElementById("filter-department") ? document.getElementById("filter-department").value : "";
    const credits = document.getElementById("filter-credits") ? document.getElementById("filter-credits").value : "";
    const professor = document.getElementById("filter-professor") ? document.getElementById("filter-professor").value : "";
    const params = new URLSearchParams({ department, credits, professor });

    fetch(`/course-portal/StudentDashServlet?${params.toString()}`)
        .then(response => response.json())
        .then(data => {
            const tbody = document.querySelector("#catalog-table tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6">No courses found.</td></tr>`;
                return;
            }
            data.forEach(course => {
                tbody.innerHTML += `
                    <tr>
                        <td>${course.code}</td>
                        <td>${course.title}</td>
                        <td>${course.credits}</td>
                        <td>${course.department}</td>
                        <td>${course.semester_num}</td>
                        <td>${course.professor}</td>
                    </tr>`;
            });
        });
}

function fetchEnrolledCourses() {
    fetch(`/course-portal/StudentDashServlet?action=enrolled`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#enrolled-table tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8">No enrolled courses.</td></tr>`;
                return;
            }
            data.forEach(course => {
                let day = "", start = "", end = "";
                if (course.schedules && course.schedules.length > 0) {
                    day = course.schedules[0].day || "";
                    start = course.schedules[0].start_time || "";
                    end = course.schedules[0].end_time || "";
                }
                tbody.innerHTML += `
                    <tr>
                        <td>${course.code}</td>
                        <td>${course.title}</td>
                        <td>${course.credits}</td>
                        <td>${course.department}</td>
                        <td>${course.professor}</td>
                        <td>${day}</td>
                        <td>${start}</td>
                        <td>${end}</td>
                    </tr>`;
            });
        });
}

function fetchResults() {
    fetch(`/course-portal/StudentDashServlet?action=results`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#results-table tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6">No results found.</td></tr>`;
                return;
            }
            data.forEach(result => {
                tbody.innerHTML += `
                    <tr>
                        <td>${result.code}</td>
                        <td>${result.title}</td>
                        <td>${result.credits}</td>
                        <td>${result.department}</td>
                        <td>${result.professor}</td>
                        <td>${result.grade || 'N/A'}</td>
                    </tr>`;
            });
        });
}


function registerCourseByCode() {
    const code = document.getElementById("register-course-code").value.trim();
    const msgDiv = document.getElementById("register-message");
    msgDiv.textContent = "";
    msgDiv.style.color = "";
    if (!code) {
        showMessage(msgDiv, "Please enter a course code.", "red", "registerMsgTimeout");
        return;
    }
    fetch("/course-portal/StudentDashServlet", {
        method: "POST",
        headers: {"Content-Type": "application/x-www-form-urlencoded"},
        body: `action=registerByCode&code=${encodeURIComponent(code)}`
    })
    .then(res => res.json())
    .then(data => {
        if (data.message) {
            showMessage(msgDiv, data.message, "green", "registerMsgTimeout");
        } else if (data.error) {
            showMessage(msgDiv, data.error, "red", "registerMsgTimeout");
        } else {
            showMessage(msgDiv, "Registration failed.", "red", "registerMsgTimeout");
        }
        fetchEnrolledCourses();
        fetchCoursesForDrop();
    })
    .catch(() => {
        showMessage(msgDiv, "Registration failed.", "red", "registerMsgTimeout");
    });
}


function fetchCoursesForDrop() {
    fetch(`/course-portal/StudentDashServlet?action=dropList`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#drop-table tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6">No courses to drop.</td></tr>`;
                return;
            }
            data.forEach(course => {
                tbody.innerHTML += `
                    <tr>
                        <td>${course.code}</td>
                        <td>${course.title}</td>
                        <td>${course.credits}</td>
                        <td>${course.department}</td>
                        <td>${course.professor}</td>
                        <td>
                            <button class="drop-btn" data-id="${course.course_id}">Drop</button>
                        </td>
                    </tr>`;
            });
           
            document.querySelectorAll(".drop-btn").forEach(btn => {
                btn.addEventListener("click", function () {
                    dropCourse(this.getAttribute("data-id"));
                });
            });
        });
}

function dropCourse(courseId) {
    const msgDiv = document.getElementById("drop-message");
    msgDiv.textContent = "";
    msgDiv.style.color = "";
    fetch("/course-portal/StudentDashServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `action=drop&course_id=${courseId}`  
    })
    .then(res => res.json())
    .then(data => {
        if (data.message) {
            showMessage(msgDiv, data.message, "green", "dropMsgTimeout");
        } else if (data.error) {
            showMessage(msgDiv, data.error, "red", "dropMsgTimeout");
        } else {
            showMessage(msgDiv, "Drop failed.", "red", "dropMsgTimeout");
        }
        fetchCoursesForDrop(); 
        fetchEnrolledCourses();
    })
    .catch(() => {
        showMessage(msgDiv, "Drop failed.", "red", "dropMsgTimeout");
    });
}