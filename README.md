# Кафе дъската

Дигиталната версия на дъската за кафе в офиса. Всеки колега има профил, купува предварително
пакет кафета (например 5 кафета за 2 €) и с един бутон отбелязва всяко изпито кафе.
Администраторът вижда кой колко кафета има и колко пари са събрани за месеца.

**Стек:** Vue 3 + Vite + Pinia · Spring Boot 3.3 (Java 21) + Spring Security (JWT) + JPA · PostgreSQL 16 + Flyway · Docker Compose + nginx

## Какво може

**Колега**
- Вход с потребителско име и парола
- Вижда баланса си като число и като чертички, както на старата дъска
- Купува пакет кафета, после оставя парите в касичката
- „Изпих кафе“ намалява баланса с 1. Грешно натискане се отменя до 10 минути
- Обща бяла дъска с всички активни колеги и техните чертички (колега без кафета стои с червена 0, админът може да скрие някого). Изпито кафе изтрива една чертичка, а дъската се опреснява сама на 20 секунди. С маркерите на поставката може да се рисува по дъската, а гъбата трие рисунките
- История на покупките и изпитите кафета
- Смяна на собствената парола

**Администратор** (вижда и всичко, което вижда колегата)
- Месечен отчет: събрана сума, продадени и изпити кафета, общо предплатени и още неизпити кафета
- Таблица „кой колко кафета има“ с движението за месеца, сортира се по всяка колона
- Навигация по месеци назад във времето
- Списък с покупките за месеца и изтриване на погрешни. Кафетата се махат от баланса, ако още не са изпити
- Създаване на колеги, смяна на име, роля, деактивиране, нова парола
- „Запиши плащане“ за колега, който е дал парите директно на админа
- „Безкрайни кафета“ за отделен колега: балансът му не намалява, на дъската стои ∞, а изпитите му кафета пак се виждат в отчета
- Управление на пакетите (име, брой, цена, видимост). Старите покупки не се променят

## Пускане на сървъра

Нужни са Docker и Docker Compose v2.

```bash
# 1. Копирай проекта на сървъра и влез в папката
cd coffee-office

# 2. Създай .env
cp .env.example .env
openssl rand -base64 48        # сложи резултата в JWT_SECRET
nano .env                      # попълни паролите

# 3. Билд и старт
docker compose up -d --build

# 4. Логове (първото стартиране на бекенда отнема ~20-30 сек.)
docker compose logs -f backend
```

Отвори `http://<сървър>:8080` и влез с `ADMIN_USERNAME` / `ADMIN_PASSWORD` от `.env`.
След първия вход смени паролата от „Профил“. Администраторът се създава само при първо
стартиране, ако няма активен админ, така че смяната на `ADMIN_PASSWORD` по-късно няма ефект.

По подразбиране базата е заредена с два пакета: 5 кафета за 2,00 € и 10 кафета за 4,00 €.
Промени ги от „Админ → Пакети“.

### Обновяване
```bash
git pull   # или копирай новите файлове
docker compose up -d --build
```
Миграциите на базата (Flyway) се изпълняват автоматично при старт.

### Бекъп на базата
```bash
docker compose exec db pg_dump -U coffee coffee > backup-$(date +%F).sql
# възстановяване:
cat backup.sql | docker compose exec -T db psql -U coffee coffee
```

### HTTPS
Ако сървърът е достъпен извън офисната мрежа, сложи го зад reverse proxy с TLS
(Caddy, Traefik или nginx + Let's Encrypt), защото паролите минават при вход.

## Локална разработка

```bash
# База
docker run -d --name coffee-db -p 5432:5432 \
  -e POSTGRES_DB=coffee -e POSTGRES_USER=coffee -e POSTGRES_PASSWORD=coffee postgres:16-alpine

# Бекенд (localhost:8080, админ admin / admin123 по подразбиране)
cd backend && mvn spring-boot:run

# Фронтенд (localhost:5173, /api се проксира към 8080)
cd frontend && npm install && npm run dev
```

## Структура

```
coffee-office/
├── docker-compose.yml
├── .env.example
├── backend/                         Spring Boot
│   ├── Dockerfile
│   └── src/main/
│       ├── java/bg/office/coffee/
│       │   ├── config/              SecurityConfig, AdminSeeder
│       │   ├── security/            JWT филтър и сървис
│       │   ├── domain/              AppUser, CoffeePackage, Purchase, Consumption
│       │   ├── repo/                Spring Data репозиторита
│       │   ├── service/             CoffeeService, StatsService, UserService, PackageService
│       │   └── web/                 REST контролери, DTO-та, обработка на грешки
│       └── resources/
│           ├── application.yml
│           └── db/migration/        Flyway SQL миграции
└── frontend/                        Vue 3
    ├── Dockerfile, nginx.conf
    └── src/
        ├── views/                   Login, Home, Account, admin/*
        ├── components/              WhiteBoard, TallyMarks, ConfirmDialog, ToastStack
        ├── stores/auth.js           Pinia – токен и текущ потребител
        └── api.js                   fetch обвивка с JWT
```

## API

Всички заявки освен вход изискват `Authorization: Bearer <token>`.

| Метод | Път | Описание |
|---|---|---|
| POST | `/api/auth/login` | Вход → `{ token, user }` |
| GET | `/api/me` | Текущ потребител и баланс |
| GET | `/api/board` | Общата дъска: активни колеги с баланс > 0 |
| GET | `/api/packages` | Активни пакети |
| POST | `/api/me/purchases` | Купи пакет `{ packageId }` |
| POST | `/api/me/consumptions` | Изпих кафе (−1) |
| DELETE | `/api/me/consumptions/last` | Отмени последното (до 10 мин.) |
| GET | `/api/me/activity` | Последни 30 движения |
| POST | `/api/me/password` | Смяна на парола |
| GET | `/api/admin/stats?year=&month=` | Месечен отчет |
| GET | `/api/admin/purchases?year=&month=` | Покупки за месеца |
| DELETE | `/api/admin/purchases/{id}` | Изтрий покупка |
| GET/POST | `/api/admin/users` | Списък / нов колега |
| PUT | `/api/admin/users/{id}` | Име, роля, активност |
| POST | `/api/admin/users/{id}/reset-password` | Нова парола |
| POST | `/api/admin/users/{id}/purchases` | Запиши плащане за колега |
| GET/POST | `/api/admin/packages` | Пакети |
| PUT | `/api/admin/packages/{id}` | Промени пакет |

## Бизнес правила накратко

- Балансът не може да стане отрицателен. Намаляването е атомарна SQL заявка, затова две бързи натискания не могат да „изпият“ повече от наличното.
- Покупката копира името, броя и цената на пакета. Ако после промениш пакета, историята и отчетите не се променят.
- Пакетите не се трият, а се скриват.
- Месецът в отчета се смята по часовата зона от `APP_TIMEZONE` (Europe/Sofia).
- Деактивиран колега не може да влиза. В отчета се показва, само ако има баланс или движение през месеца.
