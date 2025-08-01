# ![Queermap Banner](Queermapbanner.png)

# Queermap

> **Una app que geolocaliza espacios seguros, culturales, de salud y ocio para la comunidad LGBTIQ+ en Chile.**

---

<div align="justify">

En un mundo donde la diversidad y la inclusión son valores fundamentales, surge **Queermap** como una herramienta transformadora para la comunidad LGBTIQ+. Esta aplicación nace de la necesidad de contar con espacios seguros, acogedores y visibilizados, donde todas las personas puedan ser plenamente ellas mismas.

**Queermap** conecta personas con servicios locales confiables en salud, entretenimiento, cultura, educación, orientación y seguridad.

---

## 🚀 Estado Actual del Proyecto

- Arquitectura limpia y modular basada en **Clean Architecture** y **MVVM**.
- Implementación completa del **login** con correo electrónico mediante **Firebase Authentication**.
- Pantallas iniciales: **Splash** y **Cover de bienvenida**.
- Cobertura de tests unitarios y de UI en componentes clave (`Login`, `MainActivity`).
- Inyección de dependencias con **Hilt** en integración inicial.
- Flujos sociales integrados: login con Google y Facebook.
- Estado estable y pruebas automatizadas con **JaCoCo** > 95% instrucciones.

### Funcionalidades en desarrollo / pendientes

- Mapa interactivo con geolocalización de espacios seguros.
- Perfil de usuario y guardado de lugares favoritos.
- Funcionalidades sociales (comentarios, comunidades).
- Notificaciones y alertas.
- Botón de emergencia para apoyo en crisis.
- Demarcación de zonas de riesgo.

---

## 🧰 Stack Tecnológico y Arquitectura

- **Lenguaje:** Kotlin
- **Arquitectura:** Clean Architecture con separación clara en `domain`, `data`, `ui` y `di`.
- **Patrón:** MVVM con gestión reactiva de estado usando Flows/Coroutines.
- **Inyección de dependencias:** Hilt/Dagger.
- **Autenticación:** Firebase Authentication (email/password, Google, Facebook).
- **Navegación:** Jetpack Navigation Component.
- **Testing:** Unit tests, Instrumented UI tests con Espresso y cobertura con JaCoCo.
- **Modularización:** Capas y módulos separados para facilitar escalabilidad y mantenimiento.

---

## 📖 Buenas Prácticas Implementadas

- **Testing exhaustivo:**  
  Cobertura de código sobre 95%, con tests unitarios en lógica de negocio y pruebas instrumentadas en UI para flujos críticos.

- **Manejo de estados UI:**  
  Uso de estados inmutables y eventos para comunicación unidireccional entre ViewModel y UI.

- **Seguridad y privacidad:**  
  Manejo correcto de datos sensibles y flujos seguros de autenticación social.

- **UX adaptable:**  
  El diseño UI se adapta a distintos estados (login tradicional, social, errores de validación, carga).

- **Código limpio y mantenible:**  
  Aplicación de principios SOLID, separación de responsabilidades y modularización.

---

## 🔗 Enfoque Social y Misión

Queermap no solo es tecnología; es una herramienta de impacto social. Busca ser un **espacio seguro digital** que apoye a la comunidad LGBTIQ+ en Chile, visibilizando servicios y lugares amigables, fomentando la inclusión, el respeto y la solidaridad. Además, ofrece recursos para prevenir la discriminación, violencia y crisis de salud mental.

---

## 👩‍💻 Cómo Colaborar

¡Queremos que formes parte de este proyecto! Para contribuir:

1. Haz un fork del repositorio.
2. Crea una rama para tu feature o fix:  
   `git checkout -b feature/nombre-de-tu-feature`
3. Realiza tus cambios y pruebas.
4. Envía un pull request con descripción clara de tus aportes.

---

## 📬 Contacto

Si quieres saber más, colaborar o aportar ideas, ¡escríbeme!

[![LinkedIn](https://img.shields.io/badge/-javimutis-blue?style=flat-square&logo=Linkedin&logoColor=white&link=https://www.linkedin.com/in/javimutis/)](https://www.linkedin.com/in/javimutis/)  
[![Instagram](https://img.shields.io/badge/-javi.mutis-E4405F?style=flat-square&logo=instagram&logoColor=white&link=https://www.instagram.com/javi.mutis/)](https://www.instagram.com/javi.mutis/)  
[![GitHub](https://img.shields.io/badge/-javimutis-black?style=flat-square&logo=github&logoColor=white&link=https://github.com/javimutis)](https://github.com/javimutis)  
[![Email](https://img.shields.io/badge/-javimutisdev%40gmail.com-red?style=flat-square&logo=gmail&logoColor=white&link=mailto:javimutisdev%40gmail.com)](mailto:javimutisdev@gmail.com)

---

## ❤️ Agradecimientos

Gracias a todas las personas y organizaciones que apoyan con tiempo, ideas y pasión para construir un espacio más inclusivo y seguro para la comunidad LGBTIQ+.

---

</div>
