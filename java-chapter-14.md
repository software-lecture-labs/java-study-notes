<a id="top"></a>

# Bölüm 14: Uygulama Sunucusuna Geçiş: CDI, JTA ve JAX-RS ile Payara Micro

<!-- nav:top:start -->
[← Bölüm 13](java-chapter-13.md#top) · [İçindekiler](README.md#top)
<!-- nav:top:end -->

Son dört bölümde bilinçli olarak zor yolu seçtik. Nesneleri birbirine bağlayan **composition root** sınıfını *(SalesModule)* elle yazdık, domain olaylarını dağıtan bir yayıncı *(InProcessEventPublisher)* geliştirdik, veritabanı bağlantısını ortam değişkenlerinden okuyan bir **Database** sınıfı kurguladık, transaction'ları repository içinde `setAutoCommit(false)` ve `commit()` çağrılarıyla yönettik ve son olarak JDK'nın basit HTTP sunucusu üzerine **regex** ile yönlendirme yapan, **JSON** dönüşümünü, doğrulamayı ve hata eşlemesini tek tek ele alan bir **HTTP katmanı** yazdık. Amacımız bir uygulama sunucusunun bizim yerimize ne yaptığını görmekti.

Artık bu işleri kurumsal standartlara göre asıl sorumlularına devredebiliriz. Bu bölümde `sales` modülünü **Payara Micro** üzerinde çalışan bir web uygulamasına *(WAR)* dönüştüreceğiz. Elle yazdığımız her parçanın **Jakarta EE** ve **MicroProfile** tarafında bir karşılığı var. Yol haritamızı aşağıdaki tablo ile özetleyebiliriz.

| **Elle yazdığımız** | **Uygulama sunucusundaki karşılığı** |
| --- | --- |
| **SalesModule *(composition root)*** ve constructor injection | **CDI** *(`@ApplicationScoped`, `@Inject`, `@Produces`)* |
| **InProcessEventPublisher** ve abonelik listesi | **CDI Events** *(`Event<T>`, `@Observes`)* |
| `Database.fromEnvironment(...)` ve **PGSimpleDataSource** | `@DataSourceDefinition` + **MicroProfile Config** |
| Repository içindeki `setAutoCommit(false)` / `commit()` / `rollback()` | **Jakarta Transactions** *(JTA, `@Transactional`)* |
| `Database.migrated(...)` çağrısı | Uygulama açılışında çalışan bir CDI gözlemcisi *(`@Observes Startup`)* |
| **SalesHttpServer** ve regex ile yönlendirme | **Jakarta REST** *(JAX-RS, `@Path`, `@GET`, `@POST`)* |
| **JsonCodec *(Jackson)*** | **JSON-B** *(Payara içinde Yasson)* |
| `RequestValidator.validate(...)` | Parametre üzerinde `@Valid` |
| `OrderResource.handle` içindeki tek `catch` bloğu | `ExceptionMapper<T>` ve `@Provider` |
| `SalesApi.main` ve **CountDownLatch** | `java -jar payara-micro.jar --deploy sales.war` |

Bu bölüm için şimdilik JDBC repository'lerimiz yerinde kalıyor. SQL cümlelerine, `update-then-insert` yaklaşımına ve sözleşme testlerine dokunmayacağız. Değişecek olan şey SQL'in etrafındaki altyapı. Yani bağlantıyı kimin verdiği, transaction'ı kimin yönettiği ve nesneleri kimin oluşturduğu gibi. JPA'yı ise kendi başına ele alınması gereken bir konu olarak sonraki bölümlere bırakıyoruz. Bu bölümün amacı bir nevi aşağıdaki sorulara cevap bulmak.

- Bir framework'e geçtiğimizde domain modelimiz ne kadar değişmeli? Hangi paketler framework'ü tanıyabilir, hangileri tanımaMAlıdır?
- Composition root ortadan kalkar mı, yoksa sadece ufalır mı?
- Transaction sınırı neden repository'den kullanım senaryosuna *(use case)* taşınmalıdır?
- Bir olay *(domain event)* ne zaman yayınlanmalıdır? Veritabanına yazılmadan önce mi, yazıldıktan sonra mı?
- Önceki bölümde yazdığımız sınıflardan hangileri `JAX-RS` bileşeni olarak yaşamaya devam eder, hangileri ortadan kalkar?

// DEVAM EDECEK

<!-- nav:bottom:start -->
---

| ← Önceki Bölüm | İçindekiler | Sonraki Bölüm → |
| :--- | :---: | ---: |
| [Bölüm 13: HTTP Boundary, DTO, Validasyon ve Hata Sözleşmeleri](java-chapter-13.md#top) | [Ders Notları](README.md#top) | |
<!-- nav:bottom:end -->
