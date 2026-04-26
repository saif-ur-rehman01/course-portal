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
            if (this.dataset.tab === "catalog") fetchCatalog();
            if (this.dataset.tab === "students") fetchStudents();
            if (this.dataset.tab === "timetable") fetchTimetable();
        });
    });

    fetchProfessorName();
    fetchCatalog();

    document.getElementById("logout-btn").addEventListener("click", function () {
        fetch("/course-portal/logout", { method: "POST" })
            .then(() => {
                window.location.href = "index.html";
            });
    });

   
    document.getElementById("upload-grade-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const msgDiv = document.getElementById("upload-grade-msg");
        msgDiv.textContent = "";
        msgDiv.style.color = "";
        const username = document.getElementById("grade-student-username").value.trim();
        const code = document.getElementById("grade-course-code").value.trim();
        const grade = document.getElementById("grade-value").value.trim();
        const params = new URLSearchParams({ action: "uploadGrade", username, code, grade });
        try {
            const res = await fetch("/course-portal/ProfessorDashServlet", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: params.toString()
            });
            const data = await res.json();
            if (data.message) {
                msgDiv.textContent = data.message;
                msgDiv.style.color = "green";
            } else if (data.error) {
                msgDiv.textContent = data.error;
                msgDiv.style.color = "red";
            } else {
                msgDiv.textContent = "Unknown error.";
                msgDiv.style.color = "red";
            }
            this.reset();
        } catch {
            msgDiv.textContent = "Network error.";
            msgDiv.style.color = "red";
        }
    });
});

function fetchProfessorName() {
    fetch("/course-portal/ProfessorDashServlet?action=session")
        .then(res => res.json())
        .then(data => {
            if (data && data.name) {
                document.getElementById("professor-name").textContent = "Welcome, " + data.name;
            }
        });
}

function fetchCatalog() {
    fetch("/course-portal/ProfessorDashServlet?action=catalog")
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#catalog-table tbody");
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5">No courses found.</td></tr>`;
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
                    </tr>`;
            });
        });
}

function fetchStudents() {
    fetch("/course-portal/ProfessorDashServlet?action=students")
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#students-table tbody");
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6">No students found.</td></tr>`;
                return;
            }
            data.forEach(stu => {
                tbody.innerHTML += `
                    <tr>
                        <td>${stu.student_id}</td>
                        <td>${stu.full_name}</td>
                        <td>${stu.username}</td>
                        <td>${stu.email}</td>
                        <td>${stu.course_code}</td>
                    </tr>`;
            });
        });
}

function fetchTimetable() {
    fetch("/course-portal/ProfessorDashServlet?action=timetable")
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#timetable-table tbody");
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6">No timetable found.</td></tr>`;
                return;
            }
            data.forEach(row => {
                tbody.innerHTML += `
                    <tr>
                        <td>${row.course_code}</td>
                        <td>${row.day}</td>
                        <td>${row.start_time}</td>
                        <td>${row.end_time}</td>
                    </tr>`;
            });
        });
}