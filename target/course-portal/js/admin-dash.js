// ------- Tab Switching -------
document.querySelectorAll(".tab-button").forEach(btn =>
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-button").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(tab => tab.classList.remove("active"));

    btn.classList.add("active");
    document.getElementById(btn.dataset.tab).classList.add("active");
  })
);

// ------- Shared fetcher helper -------
async function postForm(endpoint, form) {
  const params = new URLSearchParams();
  for (const [k, v] of new FormData(form).entries()) {
    params.append(k, v);
  }
  const res = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  });
  return res.text();
}

// ------- User Management -------
document.querySelector('#add-user-form').addEventListener('submit', async e => {
  e.preventDefault();
  const msg = await postForm('/course-portal/add-users', e.target);
  alert(msg);
  e.target.reset();
  fetchUsers();
});

document.querySelector('#delete-user-form').addEventListener('submit', async e => {
  e.preventDefault();
  const msg = await postForm('/course-portal/delete-users', e.target);
  alert(msg);
  e.target.reset();
  fetchUsers();
});

async function fetchUsers() {
  try {
    const res = await fetch('/course-portal/manage-users');
    const users = await res.json();
    const tbody = document.querySelector("#users-table tbody");
    tbody.innerHTML = users.map(u => `
      <tr>
        <td>${u.user_id}</td>
        <td>${u.username}</td>
        <td>${u.full_name}</td>
        <td>${u.email}</td>
        <td>${u.role}</td>
      </tr>`).join('');
  } catch (err) {
    console.error("Failed to load users", err);
    alert("Error fetching users");
  }
}

document.querySelector('#view-user-form').addEventListener('submit', async e => {
  e.preventDefault();
  const params = new URLSearchParams(new FormData(e.target));
  try {
    const res = await fetch('/course-portal/view-users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });
    const users = await res.json();
    const tbody = document.querySelector("#users-table tbody");
    tbody.innerHTML = users.map(u => `
      <tr>
        <td>${u.user_id}</td>
        <td>${u.username}</td>
        <td>${u.full_name}</td>
        <td>${u.email}</td>
        <td>${u.role}</td>
      </tr>`).join('');
  } catch (err) {
    console.error("Filtered users failed", err);
    alert("Error filtering users");
  }
});


document.querySelector('#add-course-form').addEventListener('submit', async e => {
  e.preventDefault();
  const msg = await postForm('/course-portal/add-course', e.target);
  alert(msg);
  e.target.reset();
  fetchCourses();
});

document.querySelector('#delete-course-form').addEventListener('submit', async e => {
  e.preventDefault();
  const msg = await postForm('/course-portal/delete-course', e.target);
  alert(msg);
  e.target.reset();
  fetchCourses();
});

document.querySelector('#view-courses-form').addEventListener('submit', async e => {
  e.preventDefault();
  const params = new URLSearchParams(new FormData(e.target));
  try {
    const res = await fetch('/course-portal/view-courses', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });
    const data = await res.json();
    if (!data.success) {
      alert(data.error || "Failed to fetch courses.");
      return;
    }
    const tbody = document.querySelector("#courses-table tbody");
    tbody.innerHTML = data.courses.map(c => `
      <tr>
        <td>${c.course_id}</td>
        <td>${c.code}</td>
        <td>${c.title}</td>
        <td>${c.credits}</td>
        <td>${c.department_name}</td>
        <td>${c.professor_id || 'N/A'}</td>
      </tr>`).join('');
  } catch (err) {
    console.error("Failed fetching courses", err);
    alert("Error fetching courses");
  }
});

document.querySelector('#remove-grade-form').addEventListener('submit', async e => {
  e.preventDefault();
  const msgDiv = document.getElementById("remove-grade-msg");
  msgDiv.textContent = "";
  msgDiv.style.color = "";
  const params = new URLSearchParams(new FormData(e.target));
  try {
    const res = await fetch('/course-portal/remove-grade', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });
    const text = await res.text();
    if (text.toLowerCase().includes("success")) {
      msgDiv.textContent = text;
      msgDiv.style.color = "green";
    } else {
      msgDiv.textContent = text;
      msgDiv.style.color = "red";
      alert(text);
    }
    e.target.reset();
  } catch (err) {
    msgDiv.textContent = "Error removing grade";
    msgDiv.style.color = "red";
    alert("Error removing grade");
  }
});


fetchUsers();
fetchCourses();

async function fetchCourses() {
  try {
    const res = await fetch('/course-portal/view-courses', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: ''
    });
    const data = await res.json();
    if (!data.success) return;
    const tbody = document.querySelector("#courses-table tbody");
    tbody.innerHTML = data.courses.map(c => `
      <tr>
        <td>${c.course_id}</td>
        <td>${c.code}</td>
        <td>${c.title}</td>
        <td>${c.credits}</td>
        <td>${c.department_name}</td>
        <td>${c.professor_id || 'N/A'}</td>
      </tr>`).join('');
  } catch (err) {
    console.error("Fetch courses failed", err);
  }
}
