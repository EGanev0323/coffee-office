# Кафе дъската (coffee-office)

Офисна система за кафе, която замества бялата дъска, на която колегите пишеха кой колко кафета има.
Колегите купуват предварително пакети кафета (напр. 5 кафета за 2 €) и отбелязват всяко изпито кафе.
Админът вижда кой колко има и колко пари са събрани за месеца.

Езикът на интерфейса, съобщенията за грешки, валидациите и коментарите в кода е **български**.
Отговаряй на собственика на проекта (Емил) на български.

## Стек

- **Backend:** Java 21, Spring Boot 3.3, Spring Security (stateless JWT, jjwt 0.12), Spring Data JPA / Hibernate, Flyway, PostgreSQL 16
- **Frontend:** Vue 3 (`<script setup>`), Vite 5, Pinia, Vue Router, без UI библиотеки. Шрифтове през `@fontsource` (Onest, Caveat). `qrcode` за QR етикета.
- **Инфраструктура:** Docker Compose (`db`, `backend`, `frontend`). nginx във `frontend` контейнера сервира Vue и проксира `/api` към `backend:8080`. Публичен достъп през Tailscale Funnel (HTTPS).

## Структура

```
backend/src/main/java/bg/office/coffee/
  config/      SecurityConfig, AdminSeeder (създава първия админ от env)
  security/    JwtService, JwtAuthFilter, AuthUser, TokenClaims
  domain/      AppUser, CoffeePackage, Purchase, Consumption, BoardStroke, Role
  repo/        Spring Data репозиторита
  service/     CoffeeService, StatsService, UserService, PackageService, DrawingService, BoardEvents (SSE)
  web/         контролери (Auth, Me, Admin, Board), dto/ (records), ApiExceptionHandler
backend/src/main/resources/
  application.yml
  db/migration/   V1__init … V5__board_visible  (следващата е V6)
frontend/src/
  api.js            единствената обвивка около fetch (токен, 401, четими грешки)
  boardEvents.js    SSE клиент през fetch + reconnect
  stores/auth.js    токен (localStorage при „Запомни ме“, иначе sessionStorage)
  router.js         guard: вход, зареждане на /me, админ маршрути
  notify.js, confirm.js   toast и диалог за потвърждение
  format.js         пари (EUR, bg-BG), дати, „1 кафе / N кафета“
  components/       WhiteBoard, TallyMarks, DrawingLayer, DrinkOverlay, ConfirmDialog, ToastStack
  views/            Login, Home, Account, Drink (QR), admin/{Overview, Users, Packages, Qr, Layout}
  assets/coffee-face.jpg   лична снимка за „Ти изпи кафе“ – хранилището е private
frontend/nginx.conf   отделен location за /api/board/events (SSE, без буфериране)
docker-compose.yml, .env.example
```

## Команди

```bash
# Проверки, които трябва да минат преди всяка завършена задача
cd backend && mvn -B -q package -DskipTests
cd frontend && npm run build

# Локална разработка
docker run -d --name coffee-db -p 5432:5432 \
  -e POSTGRES_DB=coffee -e POSTGRES_USER=coffee -e POSTGRES_PASSWORD=coffee postgres:16-alpine
cd backend && mvn spring-boot:run          # :8080, админ admin / admin123 по подразбиране
cd frontend && npm install && npm run dev  # :5173, /api се проксира към :8080

# Целият стек като на сървъра
docker compose up -d --build
docker compose logs -f backend
```

Frontend Dockerfile-ът ползва `npm ci`. При добавяне на npm пакет **винаги** обновявай и `package-lock.json`
(`npm install <пакет>`), иначе билдът на сървъра пада.

Тестове: `backend/src/test` (JUnit 5, `@SpringBootTest` + профил `test`). Базата идва от Testcontainers JDBC URL
(`application-test.yml`, нужен е Docker): `cd backend && mvn -B test`. Без Docker – срещу локален PostgreSQL:
`mvn -B test -Dspring.datasource.url=jdbc:postgresql://localhost:5432/coffee_test`.
При нова логика в бекенда добавяй тестове там. Във фронтенда – Vitest, ако се добави.

## Правила за backend

- Без Lombok. DTO-тата са `record` със статичен `from(...)`. Constructor injection.
- Контролерите са тънки, логиката и `@Transactional` са в сървисите.
- Грешки: `BusinessException` (400), `NotFoundException` (404), `UnauthorizedException` (401) →
  `ApiExceptionHandler` връща `{ "message": "..." }`. Съобщенията са на български и са подходящи за показване на потребителя.
- Съобщенията във валидационните анотации (`@NotBlank(message = ...)`) също са на български.
- `spring.jpa.hibernate.ddl-auto: none`. **Всяка промяна в схемата е нова Flyway миграция.**
  Никога не редактирай вече приложена миграция. Следващата е `V6__...sql`.
- Балансът се променя **само** през атомарните заявки `AppUserRepository.increaseBalance/decreaseBalance`
  (`update ... where balance >= :amount`). В базата има `check (balance >= 0)`.
- Покупката копира името, броя и цената на пакета – промяна на пакет не променя историята.
- Пакетите не се трият, а се скриват (`active = false`).
- Месечните граници се смятат в `APP_TIMEZONE` (Europe/Sofia).
- Броенето на нещо, което може да е празно (`sum(...)`), връща `null` от репозиторито и се обработва в сървиса –
  без `coalesce(sum(...), 0)` в JPQL.

## Правила за frontend

- Всички заявки минават през `api()` от `api.js`. Единственото изключение е SSE в `boardEvents.js`.
- Всеки текст в интерфейса е на български.
- Дизайн токените са в `styles.css` (`--board`, `--ink`, `--marker`, `--red`, `--font-ui`, `--font-hand` …).
- Визията е „бяла дъска“: спокоен интерфейс с Onest, а само елементите „написани с маркер“ са с Caveat
  (баланс, имена и чертички на дъската, надписи под снимката). Не разпространявай Caveat в обикновения UI.
- Цветовете на маркерите са фиксирани: `#1E4BAF` (син), `#B8372A` (червен), `#2B1F18` (черен). Бекендът ги валидира
  (`DrawingService.COLORS`, плюс `ERASER`).
- Всяка анимация трябва да уважава `prefers-reduced-motion` (глобалното правило в `styles.css` + проверки в JS).
- Проверявай изгледа и на мобилен екран (~390 px). Колегите ползват основно телефони.

## Важни решения (не ги променяй без причина)

- **JWT:** HS256, claims `tv` (token_version) и `rem` (remember me). `JwtAuthFilter` зарежда потребителя от базата
  при всяка заявка и сравнява `active` и `token_version`. Смяна или нулиране на парола увеличава `token_version`
  и отписва всички устройства. `/me/password` връща нов токен за текущото устройство.
- **„Запомни ме“:** 90 дни (`JWT_REMEMBER_DAYS`), токенът е в localStorage. Без отметка – 12 часа в sessionStorage.
- **SSE (`/api/board/events`):** през `fetch` с `Authorization` хедър, не през `EventSource` – токенът не трябва
  да попада в URL. В `SecurityConfig` има `dispatcherTypeMatchers(ASYNC, ERROR).permitAll()`, иначе потокът
  получава 403 при async dispatch. Heartbeat на 25 сек. (`@EnableScheduling`).
- **QR (`/drink`):** отварянето на страницата не променя нищо; кафето се отбелязва с POST от страницата
  (линк прегледи в чат приложения не трябва да махат кафе). Отмяна 15 сек., защита от двойно сканиране до 60 сек.
  Router guard-ът слага `?at=<време на сканиране>` още преди входа; кафето се отбелязва само ако сканирането е до 10 мин.
  и само веднъж – страницата веднага сменя адреса на `?done=1`. „Назад“ или стар таб, отворен наново, показват бутон
  „Изпих кафе“, а не махат кафе сами.
- **Рисуване по дъската:** точките са нормализирани спрямо ширината на дъската. Гъбата е линия с цвят `ERASER`,
  рисувана с `globalCompositeOperation = 'destination-out'` – трие само рисунките, не и чертичките под canvas-а.
  Лимит 1000 линии.
- **Безкрайни кафета (`unlimited`):** балансът не намалява, пакети не се купуват, на дъската стои ∞,
  не се брои в „предплатени“.
- **Дъската** показва всички активни колеги с `board_visible = true`, включително тези с 0 кафета (червена 0).
- **Гъбата на дъската** трие чертичките една по една, когато балансът намалее (най-много 5 с анимация наведнъж).

## Сигурност и данни

- Тайните са само в `.env` на сървъра. Никога не commit-вай `.env` и не слагай реални тайни в `application.yml`
  (там са само dev стойности по подразбиране).
- Не пипай продукционната база и не пускай миграции ръчно – Flyway ги пуска при старт.
- Не работи като `root` върху работещия сървър. Работи в клонирано хранилище и в отделен клон за всяка задача.
- Логинът няма ограничение на опитите – ако се добави, да е в бекенда.

## Деплой

На сървъра (папката с проекта, `.env` е там и не е в git):

```bash
git pull
docker compose up -d --build
```

Специфично за сървъра (не е в git): `APP_PORT=8090`, портът на базата е вързан към Tailscale IP
(`100.64.0.20:5433:5432`) за достъп с DbVisualizer, публичният достъп е през `tailscale funnel`.

## Идеи за бъдещето (още не са решени)

- Реални плащания: Stripe с „портфейл“ (зареждане от 10 €, заради фиксираната такса) или банков превод
  с уникално основание и потвърждение от админа.
- Кафе рулетка („Кой черпи днес?“) в реално време през SSE, кафе барометър, значки като магнити,
  лични снимки и реплики за всеки колега, „Отивам за кафе, кой идва?“, кафеман на месеца.
- PWA (manifest + service worker), за да се инсталира на телефона.
