# Changelog

All notable changes to **QueerMap** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## \[Unreleased]

* Ongoing refactorings and improvements to UX/UI and test coverage.

---

## \[1.0.0] – 2025-08-01

### Added

* 📍 **Full Login System Testing**: Introduced extensive unit and instrumentation tests for `LoginFragment`, validating navigation, snackbar visibility, loading state management, and custom helper usage.
* 🔐 **Auth Use Cases Unit Tests**: Added complete coverage for `LoginWithEmail`, `LoginWithGoogle`, `LoginWithFacebook`, and `RegisterWithFacebook` use cases.
* 📄 **README Enhancements**: Updated project documentation with tech stack, architecture overview, and clearer installation instructions.

### Changed

* 🔁 **Refactor to Clean Architecture**: The entire authentication module was migrated to a clean architecture structure using domain, data, and presentation layers.
* 🧪 **Improved Test Reliability**: Test modules were optimized for better mocking and dependency injection via Hilt.
* 🧰 **JaCoCo Configuration**: JaCoCo integrated and configured for reliable unit test coverage reports.

---

## \[0.9.0] – 2025-07-01

### Added

* 🎯 **Signup Flow Refactor**: Signup flow was completely rewritten to adopt Clean Architecture and separate concerns.
* 🗺️ **Navigation Component**: Migrated from Activities to Fragments; integrated Navigation Component across Splash, Cover, and Main.
* ✨ **Material 3 Support**: Modernized calendar UI with Material Design 3 components.
* 🔍 **Form Validation**: Extracted and centralized field validation logic into `InputValidator` classes.

### Fixed

* 🐛 Issues in social login flows now provide user feedback and graceful error handling.

---

## \[0.8.0] – 2025-06-10

### Added

* ✅ **Test Coverage for ViewModels**: Added passing tests for `MainActivityViewModel`, `CoverViewModel`, `ForgotPasswordViewModel`, and initial `SignUpViewModel`.
* 🧪 **Initial LoginFragment UI Tests**: Introduced UI tests validating essential elements, login validation, and social login triggers.

---

## \[0.7.0] – 2025-05-28

### Added

* 🔐 **Google and Facebook Sign In**: Completed integration of both providers across login and signup flows.
* 🛠️ **Domain Layer Use Cases**: Created and connected domain-layer logic to data and UI layers.

---

## \[0.6.0] – 2025-05-22

### Changed

* 🧹 Major Architecture Cleanup: Refactored data, domain, UI, and DI layers for Clean Architecture, SOLID, and SRP.
* ⚡ Hilt adopted across all modules for improved DI.

---

## \[0.1.0] – 2023-05-20

### Added

* 🚀 Initial version with splash screen, welcome screen, login, and map marker features.
* 🗺️ Static map integration and dynamic markers with categories.
* 📲 Firebase integration for user registration and data sync.

## Next Steps

You may publish release notes from this changelog or generate them automatically via GitHub releases or CI.

*Thanks for contributing to QueerMap ❤️*

---

**Maintainer:** Javiera Mutis
**Last updated:** 2025-08-01
