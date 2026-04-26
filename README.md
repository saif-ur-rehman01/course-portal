# Course Portal

A full-stack web application for managing course registration for students and professors, built as a university project.

## 👥 Contributors
- Saif ur Rehman
- Haseeb Nawaz

## 🚀 Features
- Role-based login for **Students** and **Professors**
- Student course registration with **waitlist support**
- **Schedule conflict detection** — prevents overlapping class times
- Professor dashboard to manage courses and grades
- Admin panel for managing users and departments
- Prerequisite course enforcement
- Grade management system

## 🛠️ Tech Stack
- **Backend:** Java (Servlets)
- **Frontend:** HTML, CSS, JavaScript
- **Database:** PostgreSQL
- **Build Tool:** Maven

## 🗄️ Database Schema
- `users` — students and professors with role-based access
- `courses` — course catalog linked to departments and professors
- `departments` — faculty departments
- `registrations` — student enrollments with grades and waitlist status
- `course_schedule` — day and time slots per course
- `prerequisites` — course dependency mapping

---
