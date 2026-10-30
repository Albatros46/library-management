# Kütüphane Yönetimi – Mikroservis

Spring Boot 3.5 + MongoDB + React (Vite) ile yazılmış, personelin üye/kitap/ödünç işlemlerini yönettiği kütüphane uygulaması.

## Mimari

```
React (5173 / 3000) ──► API Gateway :8080 ──┬─► auth-service         :8081  staff_users
   (JWT Bearer)          JWT doğrular,       ├─► member-service       :8082  members
                         X-User-* ekler      ├─► catalog-service      :8083  books, authors, publishers
                                             ├─► loan-service         :8084  loans
                                             └─► notification-service :8085  notification_logs
loan-service ──REST──► member-service, catalog-service (üye kontrolü, stok düş/artır)
notification-service ──REST──► loan-service, member-service ──SMTP──► e-posta
```

| Servis | Sorumluluk |
|---|---|
| **api-gateway** | Spring Cloud Gateway (WebMVC). `/api/**` isteklerini yönlendirir, JWT'yi doğrular, kullanıcıyı `X-User-Id/Name/Role` başlıklarıyla iletir. İstemciden gelen sahte `X-User-*` başlıklarını siler; servisler arası uç noktaları (`/books/*/checkout`, `/books/*/return`, `/loans/*/reminded`) dışarı kapatır. |
| **auth-service** | Personel hesapları (ADMIN / STAFF), BCrypt parola, JWT üretimi. İlk açılışta yönetici hesabı oluşturur. |
| **member-service** | Üyeler: ad, soyad, e-posta, telefon, adres, durum, hatırlatma tercihi. Arama. |
| **catalog-service** | Kitap, yazar, yayınevi, kategori. Yazar detay = yazar bilgisi + kitap listesi. Kopya sayısı ve müsaitlik (atomik düş/artır). |
| **loan-service** | Ödünç iş akışı ve onaylar, son tarih takibi, gecikenler listesi, üye okuma raporu. |
| **notification-service** | Her gün 09:00'da iade tarihi yaklaşanlara e-posta, gecikenlere iade talebi e-postası; elle hatırlatma; bildirim günlüğü. |

Tüm servisler `mongodb://localhost:27017/library_management` veritabanını kullanır; her servis yalnızca kendi collection'larına yazar (yukarıdaki listede). Bir servisi ayrı veritabanına almak için o servise farklı bir `MONGO_URI` vermek yeterli. Servisler arası iletişim ilk sürümde REST; ileride Kafka/RabbitMQ olaylarına geçilebilir.

## Ödünç iş akışı

```
PENDING ──onayla──► ACTIVE ──son tarih geçti──► OVERDUE
   │                  │  ▲                         │
 reddet          iade al│  │vazgeç            iade al│
   ▼                  ▼  │                         ▼
REJECTED         RETURN_PENDING ──iadeyi onayla──► RETURNED
```

- Personel ödünç talebi açar (**PENDING**); bir personel **onaylayınca** kitabın bir kopyası düşülür ve süre başlar.
- İade: "İade al" ile **RETURN_PENDING** olur, onaylanınca kopya kataloğa geri döner. Tek adımda "İade al + onayla" da yapılabilir.
- Kurallar: üye aktif olmalı, gecikmiş kitabı olan üyeye yeni kitap verilmez, en fazla 5 açık ödünç, aynı kitaptan iki açık ödünç olmaz, 1 kez uzatma (`loan-service/application.yml`).
- Son tarihi geçen ödünçler 15 dakikada bir **OVERDUE** olarak işaretlenir ve **Gecikenler** ekranında ayrı listelenir.

## Ekranlar

Genel bakış · Ödünç işlemleri (onay bekleyenler, ödünçteki kitaplar) · Gecikenler (hatırlatma durumu, elle hatırlatma, iade al) · Üyeler / üye detayı (iletişim, üyedeki kitaplar ve iade tarihleri, geçmiş, **kaç kitap / kaç sayfa / hangi alanlarda**) · Kitap kataloğu / kitap detayı (yazar, sayfa, yayınevi, kategori, müsaitlik, kitap kimde) · Yazarlar / yazar detayı (bilgiler + kitap listesi) · Bildirimler · Personel (yalnızca yönetici).

> "Hangi alanda okuduğu" raporu, ödünç alınan kitapların kategorilerinden üretilir; kitabın gerçekten okunduğunu göstermez.

## Çalıştırma

### Docker Compose (önerilen)

```bash
docker compose up --build
```

- Arayüz: http://localhost:3000
- Gateway: http://localhost:8080
- Giden e-postalar (Mailpit): http://localhost:8025
- Giriş: `admin@library.local` / `admin123` (`ADMIN_EMAIL`, `ADMIN_PASSWORD` ile değiştirin)

Gerçek SMTP için `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_STARTTLS=true`, `MAIL_FROM` ortam değişkenlerini verin. Üretimde mutlaka `JWT_SECRET` (en az 32 bayt) ayarlayın.

### Yerel geliştirme

Gerekenler: JDK 21+, Maven, Node 20+, MongoDB (27017) ve bir SMTP sunucusu (ör. `docker run -p 1025:1025 -p 8025:8025 axllent/mailpit`).

```bash
mvn package -DskipTests
java -jar auth-service/target/app.jar
java -jar member-service/target/app.jar
java -jar catalog-service/target/app.jar
java -jar loan-service/target/app.jar
java -jar notification-service/target/app.jar
java -jar api-gateway/target/app.jar
cd frontend && npm install && npm run dev     # http://localhost:5173
```

İlk açılışta örnek veriler (3 üye, 4 yazar, 3 yayınevi, 8 kitap) yüklenir.

## Önemli API uç noktaları (gateway üzerinden)

| Metot | Yol | Açıklama |
|---|---|---|
| POST | `/api/auth/login` | Giriş, JWT döner |
| GET/POST/PUT | `/api/members`, `/api/members/{id}` | Üyeler (`?q=` arama) |
| GET/POST/PUT/DELETE | `/api/books`, `/api/books/{id}` | Kitaplar (`?q=&category=&authorId=&availableOnly=`) |
| GET | `/api/books/categories` | Kategoriler |
| GET | `/api/authors/{id}` | Yazar + kitapları |
| POST | `/api/loans` | Ödünç talebi `{memberId, bookId, days}` |
| POST | `/api/loans/{id}/approve` · `/reject` | Vermeyi onayla / reddet |
| POST | `/api/loans/{id}/return-request` · `/return-approve` · `/return-cancel` | İade akışı |
| POST | `/api/loans/{id}/extend?days=7` | Süre uzat |
| GET | `/api/loans/pending` · `/active?memberId=` · `/overdue` · `/due-soon` | Listeler |
| GET | `/api/loans/books/{bookId}/active` | Kitap kimde |
| GET | `/api/loans/members/{memberId}/stats` | Kitap sayısı, sayfa, kategori ve yazar dağılımı |
| POST | `/api/notifications/loans/{loanId}/reminder` | Elle iade hatırlatması |
| POST | `/api/notifications/run` | Günlük bildirim işini hemen çalıştır |
| GET | `/api/notifications?loanId=&memberId=` | Bildirim günlüğü |
