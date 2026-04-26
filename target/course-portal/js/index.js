function showForm(type) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.form').forEach(form => form.classList.remove('active-form'));

    if (type === 'student') {
        document.querySelector('#student-form').classList.add('active-form');
        document.querySelectorAll('.tab')[0].classList.add('active');
    } else {
        document.querySelector('#professor-form').classList.add('active-form');
        document.querySelectorAll('.tab')[1].classList.add('active');
    }
}

// Function to handle form submission (reusable for student and professor)
// ...existing code...
async function handleFormSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const params = new URLSearchParams();
    for (const [key, value] of formData.entries()) {
        params.append(key, value);
    }
    const res = await fetch("http://localhost:8080/course-portal/login", {
        method: "POST",
        body: params,
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        redirect: 'follow'
    });

    if (res.redirected) {
        window.location.href = res.url;
    } else {
        const text = await res.text();
        alert(text); // Display the full error message from the server
        console.error("Server response:", text);
    }
}

// Attach event listeners to both forms
document.getElementById("student-form").addEventListener("submit", (e) => handleFormSubmit(e, 'student'));
document.getElementById("professor-form").addEventListener("submit", (e) => handleFormSubmit(e, 'professor')); // NEW LISTENER

