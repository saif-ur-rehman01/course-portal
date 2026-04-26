document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("admin-form");
    form.addEventListener("submit", handleFormSubmit);
});

async function handleFormSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const params = new URLSearchParams();
    for (const [key, value] of formData.entries()) {
        params.append(key, value);
    }

    try {
        const res = await fetch("http://localhost:8080/course-portal/admin", {
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
            alert("Login Failed: " + text);
            console.error("Server response:", text);
        }
    } catch (err) {
        console.error("Request error:", err);
        alert("Network error. Please try again.");
    }
}
