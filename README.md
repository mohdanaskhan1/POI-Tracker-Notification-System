# 🚀 Location-Based POI Detection and Notification System

## Overview 🧭
- 🎯 Objective: design and develop a location-aware application that tracks user movement (with explicit consent) and identifies visits to predefined Points of Interest (POIs), sending a real-time “Welcome to <POI Name>” notification upon entry.
- 🗺️ Open Map API: integrates with OpenStreetMap via Overpass API to discover nearby POIs reliably and without proprietary licensing.

## Core Features ✨
- ✅ Explicit user consent for location and notifications before any tracking starts.
- 🔄 Background location tracking with periodic reporting.
- 📍 POI entry detection for categories:
  - ⛽ Fuel Stations
  - 🍽️ Restaurants
  - 🛍️ Shopping Malls/Centers
  - ➕ Additional categories can be added easily
- 🔔 Instant notification on POI entry with a friendly welcome message.
- 🔒 Secure backend with modular services and clean REST endpoints.

## Architecture 🏗️
- 🏙️ Backend: Spring Boot 3.x
  - Responsibilities: auth, location report ingestion, POI detection, visit event recording, API responses
  - Key modules: controllers, services (Overpass integration, visit tracking), repositories (JPA)
  - Database: MySQL with JPA/Hibernate for persistence of users and visit events
- 🖥️ Frontend: Vue + Vite (PWA)
  - Responsibilities: consent and settings, map view, background location reporting, notification UX
  - Service worker for caching and notifications; lightweight native background scripts for continuous tracking
- 🌐 Open Map Integration
  - Overpass API endpoints queried by category within a configurable radius around the reported location
  - Mirror fallback for resiliency; results normalized into POI DTOs

## Consent & Privacy 🔐
- 🔐 Consent Workflow
  - Location permission requested clearly and explicitly
  - Notification permission requested before sending any alerts
- 🧹 Data Minimization
  - Only essential attributes stored (user ID, device ID, POI metadata, timestamp)
  - Logs exclude sensitive payloads in production
- 🔒 Security
  - Secure password storage and standard auth flow
  - CORS and HTTPS-friendly setup

## How It Works 📍
- 🧭 Client periodically sends lat/lon and device ID to the backend.
- 🗺️ Backend queries OpenStreetMap via Overpass for nearby POIs by configured categories and radius.
- 🚪 Entry Detection
  - If distance to a POI falls below a threshold and entry state changes from “not inside” to “inside”, the backend records an “entered” visit event and the frontend shows “Welcome to <POI Name>”.
  - When leaving, a “left” event is recorded for audit and UX updates.

## API Endpoints 🛠️
- 🔑 Auth
  - POST /api/auth/login: authenticate and obtain session
- 📡 Location & Visits
  - POST /api/locations/report: submit current location; backend processes POI proximity and entry/exit
  - GET  /api/visits/recent?limit=N: fetch recent visit events for the current user
- 🗺️ POIs
  - GET  /api/pois/nearby?lat=<>&lon=<>&radius=<>&categories=...: read-only POI lookup for UI displays

## Setup ⚙️
- Prerequisites
  - ☕ Java 17+ (Spring Boot 3.x compatible)
  - 🧰 Node.js 18+ (Vite)
  - 🐬 MySQL 8.x (or compatible)
- Backend
  - Configure `backend/src/main/resources/application.yml` with your MySQL connection
  - Start: `cd backend && ./mvnw.cmd spring-boot:run` (Windows)
  - Health: `GET /api/health` (if present) to verify server status
- Frontend
  - Install: `cd frontend && npm install`
  - Run dev: `npm run dev`
  - Open the printed local URL; accept location and notification permissions when prompted
- ▶️ Quick Start
  - 1️⃣ Start backend (8080) and frontend (Vite dev server)
  - 2️⃣ Login or use the app in guest mode (if supported)
  - 3️⃣ Allow location and notifications when prompted
  - 4️⃣ Move near a POI (⛽/🍽️/🛍️) to receive “Welcome to <POI Name>” 🔔

## Non-Functional Goals 🧪
- ⚡ Low latency: efficient Overpass queries and minimal payloads
- 🛡️ Resilience: multiple Overpass mirrors, graceful fallbacks, bounded timeouts
- 🔏 Privacy: consent-first UX and reduced logging in production
- 💡 UX: responsive interface with real-time feedback

## Technical Expectations ✅
- 🔌 Effective API usage: OpenStreetMap via Overpass and RESTful backend endpoints
- 🔧 Background handling: periodic location reporting and service worker integration
- 🔔 Notifications: user opt-in, “Welcome to <POI Name>” on entry
- 🧱 Code structure: modular controllers, services, repositories, DTOs for maintainability

## Deliverables 📦
- 🧪 Working Prototype
  - Web/PWA frontend and Spring Boot backend
- 🧑‍💻 Source Code
  - Hosted on GitHub with a fresh repository history
- 🎥 Demo Presentation
  - Architecture overview, design decisions, and live demonstration of consent → tracking → POI entry → notification

## Constraints & Guidelines 📋
- ✅ Explicit consent required and enforced
- 📱 Background tracking adheres to platform constraints (browser/PWA)
- 🔌 Real-time integration preferred; mock data only when external APIs are unavailable
- 🧑‍🎓 Independently developed solution with clear structure

## Configuration Notes ⚙️
- 🗂️ Categories and detection radius are configurable server-side and can be adapted per product needs.
- 🌍 CORS allows development URLs (including ngrok) for easy mobile testing against local backend.

## License 📝
- For assignment/demo use; review before production use.

