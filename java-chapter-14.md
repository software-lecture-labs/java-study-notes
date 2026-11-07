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

## Uygulama Modülünün WAR'a Dönüşmesi

Şu ana kadar `app` modülü, `main` metodu olan sınıfların durduğu sıradan bir JAR *(Java ARchive)* çıktısıydı. Uygulama sunucuları ise bir **web arşivi** *(Web Application Archive, WAR)* kullanır. Aslında WAR, belirli bir klasör düzenine sahip bir zip dosyasından ibarettir. Kendi sınıflarımız `WEB-INF/classes`, bağımlı olduğumuz kütüphaneler `WEB-INF/lib` altına yerleşir. Sunucu bu arşivi açar, içindeki bileşenleri bulur ve onları kendi yaşam döngüsü içinde yönetir. Bu nedenle ilk iş olarak `app` modülünü bir WAR modülüne çevirelim.

Terminale yazan ve kendi kendine çalışan sınıfların bir uygulama sunucusunda pek yeri yoktur. Bu yüzden `app` modülündeki şu dosyaları siliyoruz.

| **Silinen** | **Neden Siliyoruz?** |
| --- | --- |
| `BusinessDesign.java` ve `anemic` paketi | Bölüm 9'dan beri örnek senaryoları konsolda çalıştıran giriş noktası ve anemik model karşı örnekleriydi. Snapshot'larda zaten duruyorlar. Dolayısıyla nihai projede ihtiyacımız yok. |
| `SalesApi.java` | HTTP sunucusunu başlatıp **CountDownLatch** ile bekleyen ikinci bir giriş noktasıydı. Artık bu işi **Payara Micro** yapacak. |
| `SalesModule.java` | Composition root. Görevini artık CDI *(Contexts and Dependency Injection)* devralacak ve geriye çok küçük bir sınıf kalacak. |
| `Database.java` | Bağlantı bilgilerini ortam değişkenlerinden okuyup **DataSource** üreten ve Flyway'i çalıştıran sınıf olarak kullanıldı. Bu işlerin her birinin sunucu tarafında bir karşılığı bulunuyor. |
| `ConsoleCustomerNotifier.java` | Uygulama sunucusunda terminal yok. Yerine log ortamlarına yazan bir enstrüman ekleyeceğiz. |
| `AppSmokeTest.java`, `HttpSmokeTest.java` | **SalesModule** üzerinden kurulan uçtan uca birim test sınıflarıydı. HTTP katmanını `JAX-RS`'e taşıdığımızda yerlerine yenileri gelecek. |

Dosyaları NetBeans'te sağ tıklayıp **Delete** ile ya da komut satırından aşağıdaki gibi silebiliriz. Bu adımı atlamamalıyız. Birazdan `app` modülünün `pom.xml` dosyasından H2 bağımlılığını kaldıracağız ve **Database** sınıfı yerinde kalırsa derleme `package org.h2.jdbcx does not exist` hatasıyla kırılır.

```powershell
cd src\week14-application-server\app\src
Remove-Item -Recurse test
cd main\java\com\lectures\business\design\app
Remove-Item -Recurse anemic
Remove-Item BusinessDesign.java, ConsoleCustomerNotifier.java, Database.java, SalesApi.java, SalesModule.java
```

Şu ana kadar **app** modülünü **ArchUnit** kapsamının dışında tuttuğumuzu hatırlayalım. Sistemin parçalarını birbirine bağlayan *(wiring)* yer yine burası olmaya devam edecek. Adım adım ilerleyelim. Öncelikle ana `pom.xml` dosyasına yeni sürümleri ekleyelim. **properties** kısmının sonuna aşağıdaki satırları ekleyebiliriz.

```xml
<jakartaee.version>11.0.0</jakartaee.version>
<microprofile-config.version>3.1</microprofile-config.version>
<war.plugin.version>3.4.0</war.plugin.version>
```

**dependencyManagement** sektörüne **Jakarta EE Web Profile** ve **MicroProfile Config API**'lerini ekleyelim.

```xml
<dependency>
    <groupId>jakarta.platform</groupId>
    <artifactId>jakarta.jakartaee-web-api</artifactId>
    <version>${jakartaee.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.eclipse.microprofile.config</groupId>
    <artifactId>microprofile-config-api</artifactId>
    <version>${microprofile-config.version}</version>
    <scope>provided</scope>
</dependency>
```

**pluginManagement** kısmına da **WAR** eklentisinin sürümünü ekleyelim.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-war-plugin</artifactId>
    <version>${war.plugin.version}</version>
</plugin>
```

Burada **provided** kapsamı *(scope)* oldukça önemli. Söz konusu kütüphanelere göre derleme yapıyoruz ama onları WAR'ın içine koymuyoruz. Çalışma zamanında sunucu tarafı kendi implementasyonlarını *(CDI için Weld, JAX-RS için Jersey vb.)* sağlıyor olacak *(İşte uygulama sunucuları ve sağladıkları standartların gücü)*. Aynı API'nin bir kopyasını `WEB-INF/lib` altına koyarsak sunucunun ve uygulamanın sınıf yükleyicileri *(classloader)* aynı sınıfın iki farklı sürümünü görebilir ve hiç beklenmedik **ClassCastException** hatalarıyla karşılaşabiliriz. Ayrıca tam platform yerine bilinçli olarak **Web Profile** *(`jakarta.jakartaee-web-api`)* seçtik. Nitekim **Payara Micro** da **Web Profile**'ı uygular. Böylece Web Profile'da olmayan bir API'yi *(örneğin JMS)* yanlışlıkla kullanırsak bunu derleme aşamasında fark ederiz.

`app` modülünün `pom.xml` dosyasını ise aşağıdaki hale getirelim. Paketleme türü artık `war`. Konsol testleri ortadan kalktığı için `sales` modülünün test JAR'ına ve **H2** veritabanına artık ihtiyacımız yok. **PostgreSQL** sürücüsü ise sadece çalışma zamanında gerekli.

```xml
<artifactId>week14-app</artifactId>
<packaging>war</packaging>
<name>Week 14 / app</name>

<dependencies>
    <dependency>
        <groupId>com.lectures</groupId>
        <artifactId>week14-sales</artifactId>
    </dependency>
    <dependency>
        <groupId>com.lectures</groupId>
        <artifactId>week14-shared-kernel</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>jakarta.platform</groupId>
        <artifactId>jakarta.jakartaee-web-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.eclipse.microprofile.config</groupId>
        <artifactId>microprofile-config-api</artifactId>
    </dependency>
</dependencies>

<build>
    <finalName>sales</finalName>
</build>
```

**finalName** değeri, üretilen arşivin adını `week14-app-1.0-SNAPSHOT.war` yerine `sales.war` olarak değiştiriyor. **Payara Micro** gibi uygulama sunucuları bir uygulamayı varsayılan olarak arşivin adıyla aynı **context root** altında yayınlar. Yani uygulamamız `http://localhost:8080/sales` adresinde yaşayacak. Burada bir mola verelim. Söz konusu modülü derleyip arşivin içine bakalım. WAR'ın içine bakmak sunucunun neyi göreceğini anlamanın en kısa yoludur.

```powershell
cd src\week14-application-server
mvn clean install
tar -tf app\target\sales.war
```

`WEB-INF/lib` klasöründe `week14-sales` ve `week14-shared-kernel` JAR'larının yanında Flyway, PostgreSQL sürücüsü ve bir de **Jackson** ile **Hibernate Validator** kütüphanelerini görmemiz gerekiyor. `JAX-RS`'e geçtiğimizde bunların WAR'dan çıkartılmış olduklarını tekrar kontrol etmeliyiz.

![WAR Output](./images/week_14_00.png)

## CDI ile Nesneleri Bağlamak

**OrderService** sınıfının ihtiyaç duyduğu bağımlılıkları constructor üzerinden vermiş ve tüm nesneleri **SalesModule** içinde **new** çağrıları ile oluşturmuştuk. **CDI** *(Jakarta Contexts and Dependency Injection)* aynı işi bizim yerimize yapar. Sınıfları tarar, constructor parametrelerinin tiplerine bakar, uygun nesneyi bulur veya oluşturur ve bu nesnelerin ne kadar yaşayacağına karar verir. Bunun için CDI'ın hangi sınıfları yönetmesi gerektiğini bilmesi gerekiyor.

### `beans.xml` ve Keşif Modu

`sales` modülünde `src/main/resources/META-INF` klasörünü oluşturup içine aşağıdaki **beans.xml** dosyasını ekleyelim.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                           https://jakarta.ee/xml/ns/jakartaee/beans_4_1.xsd"
       version="4.1"
       bean-discovery-mode="annotated">
</beans>
```

Aynı dosyanın bir kopyasını `app` modülünde `src/main/webapp/WEB-INF/beans.xml` olarak da ekleyelim. **bean-discovery-mode** değerinin **annotated** olması, CDI'ın sadece bean anotasyonu ile tanımlanmış sınıfları *(bean-defining annotation)* yöneteceği anlamına gelir. **all** seçeneği ise arşivdeki her sınıfı aday kabul eder. Bizim için bu tercih son derece önemli zira **CustomerRepository** arayüzünü uygulayan iki sınıfımız var. **JdbcCustomerRepository** ve **InMemoryCustomerRepository**. **all** modunda CDI bir **CustomerRepository** istendiğinde hangisini vereceğini bilemez ve uygulama açılırken *"ambiguous dependency"* hatası verir. **annotated** modunda ise sadece anotasyon eklediğimiz **JDBC** sınıfı bir bean enstrümanı haline gelir. Aynı durum **OrderIdGenerator** için de geçerlidir. Domain'deki **SequentialOrderIdGenerator** hiçbir anotasyon taşımadığı için CDI tarafında görülmez.

> Domain sınıflarına hiçbir CDI anotasyonu eklemeyeceğiz. ADR-0004'teki `domain_is_framework_free` kuralı `jakarta..` paketlerini domain ve shared-kernel için zaten yasaklıyor. Bir framework'e geçmek domain modelini değiştirmemeli. Değişiklikler domain'in etrafındaki paketlerde kalmalı.

### Kapsamlar *(Scopes)* ve Proxy Meselesi

Bir bean nesnesnin yaşam süresini belirleyen şey kapsamıdır. Bu bölümde iki kapsam ele almaktayız.

| **Kapsam** | **Ne zaman oluşturulur, ne kadar yaşar?** | **Proxy** |
| --- | --- | --- | --- |
| `@ApplicationScoped` | Uygulama boyunca tek bir örnek. İlk kullanımda oluşturulur. | Proxy vardır. Enjekte edilen şey asıl nesne değil ona yönlendiren bir **client proxy**'dir. |
| `@RequestScoped` | Her HTTP isteği için bir örnek. | Var |
| `@Dependent` | Enjekte edildiği her yer için yeni bir örnek. Yaşam süresi onu kullanan nesneye bağlıdır. | Yok |

`@ApplicationScoped` olarak işaretlenmiş bir bean'e erişen herkes aslında CDI'ın çalışma zamanında ürettiği bir alt sınıfa *(subclass)* erişir. Bu proxy, her çağrıda gerçek nesneyi bulup çağrıyı ona iletir. Bir alt sınıf üretilebilmesi için sınıfın **final** olmaması ve **private** olmayan, parametresiz bir constructor'a sahip olması gerekir. Şu anda bu tanıma uyan tüm sınıflarımızı **final** olarak yazdık. Bunu framework'ün bizden istediği ilk bedel olarak da düşünebiliriz.

**OrderService** uygulama boyunca tek bir örnek olarak yaşamalı. Sonradan ona bir **interceptor** *(`@Transactional`)* eklediğimizde bunun değeri ortaya çıkacak. **Interceptor**'ler de aynı alt sınıf tekniğiyle çalışır. Bu nedenle **OrderService** sınıfını aşağıdaki gibi düzenleyelim. Sınıfın geri kalanında bir değişiklik yok.

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderService implements OrderingFacade {

    private static final Currency TRY = Currency.getInstance("TRY");

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final OrderFactory factory;
    private final OrderMapper mapper;
    private final Clock clock;
    private final DomainEventPublisher events;

    protected OrderService() {
        this.orders = null;
        this.customers = null;
        this.factory = null;
        this.mapper = null;
        this.clock = null;
        this.events = null;
    }

    @Inject
    public OrderService(OrderRepository orders, CustomerRepository customers,
            OrderFactory factory, OrderMapper mapper, Clock clock, DomainEventPublisher events) {
        // Diğer kodlar aynı
    }
    ...
}
```

Burada dikkat edilmesi gereken birkaç nokta var.

- **final** kelimesini sınıftan kaldırdık ama alanlardan kaldırmadık. Parametresiz constructor, alanlara **null** değer atıyor. Bu constructor sadece proxy nesnesi için çalışır ve proxy hiçbir çağrıyı kendisi karşılamadan gerçek nesneye iletir. Bu sayede ADR-0006'daki `application_collaborators_are_private_final` kuralı da bozulmamış olur.
- `@Inject` anotasyonu, CDI'ın gerçek nesneyi oluştururken hangi constructor'ı kullanacağını söyler. Constructor injection yaklaşımımız aynen devam ediyor ve **OrderService**'in birim testlerinde hiçbir değişiklik gerekmiyor. Testler yine `new OrderService(...)` ile uyumlu bir şekilde çalışıyor.
- Alan *(field)* üzerine `@Inject` yazarak bu constructor'lardan kurtulabilirdik. Ancak bu durumda alanlar **final** olamaz, testlerde nesneyi oluşturmak için reflection gerekir ve bağımlılılar sınıfın imzasından kaybolur.

Repository'ler ve **OrderMapper** ise herhangi bir durum *(state)* tutmayan sınıflar. Bunları `@Dependent` olarak işaretlemek yeterli. Proxy olmadığı için **final** kalabilirler. Aşağıda **JdbcOrderRepository** için yapılan değişikliği görebiliriz. **JdbcCustomerRepository** ve **SequenceOrderIdGenerator** sınıflarında da aynı değişiklikleri yaparak devam edelim.

```java
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public final class JdbcOrderRepository implements OrderRepository {

    ...

    @Inject
    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }
```

**OrderMapper** sınıfının bir constructor'ı yok. Sadece `@Dependent` anotasyonunu eklememiz yeterli. **SalesReport** sınıfını da aynı şekilde `@Dependent` ve constructor'ında `@Inject` olacak şekilde düzenleyelim. Henüz onu kullanan bir HTTP kaynağı yok ama hazırda bulunsun.

> `@Dependent` ile işaretlenmiş bir bean'i `@ApplicationScoped` bir bean'e enjekte ettiğimizde onun da uygulama boyunca yaşayacağını unutmayalım. Repository'lerimiz state tutmadıkları için sorun yok ancak durum tutan bir `@Dependent` sınıfın tek bir örneğinin birçok isteğe birden hizmet etmesi, eş zamanlılık *(concurrency)* problemlerine yol açabilir. Buna dikkat edelim.

Bu anotasyonlar `jakarta.jakartaee-web-api` kütüphanesinden geliyor. `sales` modülünün `pom.xml` dosyasına bu bağımlılığı ekleyelim. Scope'u ana pom'da **provided** olarak tanımlandığı için burada tekrar yazmamıza gerek yok.

```xml
<dependency>
    <groupId>jakarta.platform</groupId>
    <artifactId>jakarta.jakartaee-web-api</artifactId>
</dependency>
```

### Composition Root Kayboldu mu?

CDI, anotasyon taşıyan sınıfları kendisi oluşturabilir. Peki ya anotasyon ekleyemediğimiz sınıflar ne olacak? Örneğin **OrderFactory** sınıfı domain'de duruyor ve ona anotasyon eklemek ADR-0004 ile aldığımız kararı ihlal eder. **Clock** ise JDK'ya ait bir veri yapısı. Bu tür nesneleri CDI'a bir **producer** çağrısı ile tanıtabiliriz. Bu amaçla `app` modülüne aşağıda içeriği yer alan **SalesWiring** sınıfını ekleyelim.

```java
package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;
import com.lectures.business.design.sales.application.DomainEventPublisher;
import com.lectures.business.design.sales.application.InProcessEventPublisher;
import com.lectures.business.design.sales.application.OrderActivityLog;
import com.lectures.business.design.sales.application.ShipmentNotificationHandler;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderIdGenerator;
import com.lectures.business.design.sales.domain.OrderShipped;
import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;
import javax.sql.DataSource;

@DataSourceDefinition(
        name = "java:app/jdbc/sales",
        className = "org.postgresql.ds.PGSimpleDataSource",
        url = "${MPCONFIG=sales.datasource.url}",
        user = "${MPCONFIG=sales.datasource.user}",
        password = "${MPCONFIG=sales.datasource.password}")
@ApplicationScoped
public class SalesWiring {

    @Produces
    @Resource(lookup = "java:app/jdbc/sales")
    private DataSource dataSource;

    @Produces
    Clock clock() {
        return Clock.systemUTC();
    }

    @Produces
    OrderFactory orderFactory(OrderIdGenerator ids, Clock clock) {
        return new OrderFactory(ids, clock);
    }

    @Produces
    @ApplicationScoped
    DomainEventPublisher eventPublisher(CustomerNotifier notifier) {
        return new InProcessEventPublisher()
                .subscribe(OrderEvent.class, new OrderActivityLog())
                .subscribe(OrderShipped.class, new ShipmentNotificationHandler(notifier));
    }
}
```

**orderFactory** metodunun parametrelerine dikkat edelim. **OrderIdGenerator** parametresine CDI'ın kendi bulduğu **SequenceOrderIdGenerator** örneği, **Clock** parametresine ise hemen üstteki producer'ın ürettiği nesne örneği gelecek. **Producer** metotlarının parametreleri de birer enjeksiyon noktasıdır. **eventPublisher** metodu ise abonelikleri şimdilik aynı şekilde kurmakta. Sonraki adımlarda bu metodu tamamen silip yerine CDI olaylarını yerleştireceğiz. Durumu özetleyecek olursak:

| **SalesModule'de elle yaptığımızı** | **Şimdi kim yapıyor?** |
| --- | --- |
| `new OrderService(orders, customers, factory, mapper, clock, publisher)` | CDI *(`@Inject` constructor)* |
| `new JdbcOrderRepository(dataSource)`, `new JdbcCustomerRepository(dataSource)` | CDI *(`@Dependent` + `@Inject`)* |
| `new OrderMapper()`, `new SalesReport(orders, mapper)` | CDI |
| `new OrderFactory(ids, clock)` | **SalesWiring** *(producer)* |
| `Clock.systemUTC()` | **SalesWiring** *(producer)* |
| `inMemory(...)` ve `jdbc(...)` factory metotları ile hangi implementasyonun seçileceği | `bean-discovery-mode="annotated"` *(sadece JDBC sınıfları bean)* |
| `Database.fromEnvironment(System.getenv())` | `@DataSourceDefinition` + MicroProfile Config |

Görüldüğü üzere **composition root** ortadan kalkmadı, sadece küçüldü. Elimizde kalan şey, container'ın kendi başına tahmin edemeyeceği kararlardan ibaret. Hangi saatin kullanılacağı, domain nesnelerinin nasıl oluşturulacağı veya asıl veritabanına nasıl bağlanılacağı gibi.

### Output Port İçin Yeni Bir Adaptör

**ShipmentNotificationHandler** bir **CustomerNotifier** nesnesi bekliyor. Terminale bilgi yazan sınıf artık olmadığından `app` modülüne log'a yazan bir adaptör ekleyip standartlara daha uygun bir çözüm sunabiliriz. Bunun için aşağıdaki **LoggingCustomerNotifier** sınıfını yazarak devam edelim.

```java
package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;
import jakarta.enterprise.context.Dependent;

@Dependent
public class LoggingCustomerNotifier implements CustomerNotifier {

    private static final System.Logger LOG = System.getLogger(LoggingCustomerNotifier.class.getName());

    @Override
    public void notify(String customerId, String message) {
        LOG.log(System.Logger.Level.INFO, "[notify {0}] {1}", customerId, message);
    }
}
```

JDK'nın **System.Logger** arayüzünü kullanıyoruz. **Payara** bu kayıtları **Java Util Logging *(JUL)*** üzerinden `server.log` dosyasına ve konsola yönlendirir. İlerleyen zamanlardan başka bir notifier yazarsak *(örneğin e-posta gönderen bir adaptör)* sadece bu sınıfı değiştirmemiz yeterli olacak ve **ShipmentNotificationHandler** bu değişiklikten hiç etkilenmeyecektir.

## DataSource ve MicroProfile Config

Dikkat ederseniz **SalesWiring** sınıfında `@DataSourceDefinition` anotasyonu kullanılıyor. Bu anotasyon, uygulama sunucusundan bir **bağlantı havuzu** *(connection pool)* tanımlamasını ister. Her veritabanı çağrısından fiziksel bir bağlantı açmak yerine sunucunun oluşturduğu havuzu kullanmak daha verimli. Havuz, bağlantıları açık tutar ve tekrar tekrar kullandırır. Daha da önemlisi bu bağlantıları, sonradan ele alacağımız **JTA *(Java Transaction API)*** transaction'larına dahil edebilir *(enlistment)*. Anotasyonda kullanılan özelliklere kısaca bakalım.

- **name:** Havuzun JNDI *(Java Naming and Directory Interface)* adı. `java:app/` öneki, söz konusu ismin sadece bu uygulama içinde geçerli olduğunu belirtir. **dataSource** alanındaki `@Resource(lookup = ...)` ifadesi bu adı arar, `@Produces` ise bulduğu nesneyi CDI'a tanıtır. Böylece repository nesneleri constructor'larında bir **DataSource** isteyebilir ve JNDI'dan habersiz kalabilirler.
- **className:** Havuzun kullanacağı sürücü *(driver)* sınıfı. **PostgreSQL** sürücüsü WAR'ın içinde *(`WEB-INF/lib`)* olduğu için sunucuya ayrıca bir JAR eklememize gerek yok.
- **url, user, password:** Değerleri doğrudan yazmak yerine `${MPCONFIG=...}` ifadesini kullanıyoruz. Bu Payara'ya özgü bir değişken yerine koyma *(variable substitution)* söz dizimi. Payara, söz konusu değeri **MicroProfile Config** üzerinden okur.

**MicroProfile Config**, bir ayarı farklı kaynaklardan okuyup tek bir yerden sunan bir spesifikasyon olarak düşünülebilir. Her kaynağın bir önceliği *(ordinal)* vardır ve aynı anahtar birden fazla kaynakta tanımlıysa önceliği yüksek olan değer geçerli olur.

| **Kaynak** | **Öncelik *(ordinal)*** | **Örnek** |
| --- | :---: | --- |
| Sistem özellikleri *(System properties)* | 400 | `-Dsales.demo-data=true` |
| Ortam değişkenleri *(Environment variables)* | 300 | `SALES_DEMO_DATA=true` |
| Uygulamanın içindeki `META-INF/microprofile-config.properties` | 100 | `sales.demo-data=false` |

Ortam değişkenlerinin adlandırmasına dikkat edelim. `sales.demo-data` anahtarı için MicroProfile Config sırasıyla `sales.demo-data`, `sales_demo_data` ve `SALES_DEMO_DATA` adlarına bakar. Yani daha önceden terminal penceresinden tanımladığımız `SALES_DB_URL` gibi ortam *(environment)* değişkenleri hala çalışır ama artık onları okuyan kodu biz yazmıyoruz.

Burada dikkat edilmesi gereken bir nokta var. `@DataSourceDefinition` uygulama dağıtılırken *(deployment)* işlenir. O anda WAR'ın içindeki `microprofile-config.properties` dosyası henüz okunmamıştır. Bu yüzden bağlantı bilgilerini bu dosyaya yazsak bile `${MPCONFIG=...}` bilgisi çözümlenemez. Bağlantı bilgilerini sunucu seviyesinde, yani sistem özellikleri veya ortam değişkenleri olarak vermemiz gerekir. Zaten bir parolanın uygulama arşivinin içinde durmaMAsı gerektiği için bu durum bir avantaj olarak da görülebilir. Bu sebeple `week14-application-server` klasöründe `payara` isimli bir klasör açıp içine **local.properties** dosyasını ekleyelim. Bu çalışma özelinde değerlerin kök klasördeki `docker-compose.yml` dosyasıyla uyumlu olmasına da dikkat edelim.

```properties
# Local development settings for Payara Micro, matching the root docker-compose.yml.
# Usage: java -jar payara-micro.jar --systemProperties payara/local.properties --deploy app/target/sales.war
#
# They are read as system properties, so they are visible while the application
# is being deployed: @DataSourceDefinition resolves ${MPCONFIG=...} at that point,
# before the microprofile-config.properties inside the WAR can be read.
sales.datasource.url=jdbc:postgresql://localhost:5432/postgres
sales.datasource.user=johndoe
sales.datasource.password=somew0rds
sales.demo-data=true
```

Uygulama çalışırken okunan ayarlar için ise **WAR** içindeki dosya kullanılabilir. `app` modülünde `src/main/resources/META-INF/microprofile-config.properties` dosyasını oluşturalım. Demo verilerin varsayılan olarak kapalı olduğuna dikkat edelim. **local.properties** dosyası bu değeri daha yüksek öncelikle `true` yapıyor.

```properties
# Defaults packaged with the application (ordinal 100).
# System properties (400) and environment variables (300) override them:
#   sales.demo-data  <-  -Dsales.demo-data=true  or  SALES_DEMO_DATA=true
sales.demo-data=false
```

## Açılışta Migration ve Demo Müşteri Verileri

Şu anki **SalesApi** sınıfının **main** metodu önce **Flyway migration**'larını çalıştırıyor, ardından demo amaçlı iki müşteri kaydediyor. Lakin bir WAR'ın **main** metodu bulunmuyor. Bunun yerine **CDI**, uygulama hazır olduğunda bir **Startup** event yayınlar. Bu olayı dinleyen bir metot yazarak aynı işi gerçekleştirebiliriz. `app` modülüne SalesStartup isimli sınıfı ekleyelim.

```java
package com.lectures.business.design.app;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.shared.kernel.Address;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import java.util.Objects;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

@Dependent
public class SalesStartup {

    private static final System.Logger LOG = System.getLogger(SalesStartup.class.getName());

    private final DataSource dataSource;
    private final CustomerRepository customers;
    private final boolean demoData;

    @Inject
    public SalesStartup(DataSource dataSource, CustomerRepository customers,
            @ConfigProperty(name = "sales.demo-data", defaultValue = "false") boolean demoData) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
        this.demoData = demoData;
    }

    void onStartup(@Observes Startup startup) {
        var result = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sales")
                .load()
                .migrate();
        LOG.log(System.Logger.Level.INFO, "sales schema at version {0}", result.targetSchemaVersion);

        if (demoData) {
            customers.save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
                    new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));
            customers.save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
                    new Address("Luisenstr. 48", "Munster", "44087", "Germany")));
            LOG.log(System.Logger.Level.INFO, "demo customers VINET and TOMSP are in place");
        }
    }
}
```

Kod tarafındaki bazı detayları kısaca değerlendirelim.

- **@Observes Startup::** **InProcessEventPublisher** abonelerinin CDI'daki karşılığı olan bir gözlemci metottur *(observer method)*. Olayı kimin yayınladığını bilmez ve buna gerek de yoktur. Sadece olay tipini belirtir. İlerleyen aşamalarda domain olaylarımızı da tam olarak bu şekilde dağıtacağız.
- **@ConfigProperty:** Gerekli ayarları MicroProfile Config'ten okuyup constructor parametresine enjekte eder. Anahtar hiçbir kaynakta yoksa **defaultValue** kullanılır.
- **Migration yeri:** Migration dosyaları `sales` modülünün JAR'ında *(`WEB-INF/lib/week14-sales-1.0-SNAPSHOT.jar`)* durur. Flyway bu dosyaları sınıf yolundan *(classpath)* bulabilir. Bu arada şema *(schema)* sahipliği değişmedi. Tabloları halen `sales` modülü tanımlıyor ama onları ne zaman oluşturacağına uygulama karar veriyor.

> Migration'ı uygulama açılışında çalıştırmak tek seçenek değildir. Birden fazla sunucu örneği aynı anda açılırsa hepsi migration'ı çalıştırmaya deneyebilir *(Flyway bunu veritabanı kilidi ile güvenceye alır)*. Kalabalık ekipler migration'ı genellikle dağıtım hattının *(CI/CD pipeline)* ayrı bir adımı olarak, Flyway'in Maven eklentisi veya komut satırı aracıyla işletmeyi tercih eder. *(Burada uygulanan stratejileri bir araştıralım derim)*

Şu ana kadarki adımları tamamladıysak tüm testlerin de halen geçmesi gerekiyor. **Domain**, **application** ve **persistence** testlerinin hiçbiri CDI'ın varlığından haberdar değildir. Root klasörde aşağıdaki komutu kullanarak sonuçları değerlendirelim.

```powershell
mvn clean install
```

Uygulamayı henüz bir sunucuda çalıştırmıyoruz zira HTTP katmanımız hala JDK sunucusunu kullanıyor ve WAR içinde onu başlatan bir enstrüman da yok. Sunucu ile ilk tanışmamızı transaction yönetimini de taşıdıktan sonra yapacağız.

![all tests passing](./images/week_14_05.png)

// DEVAM EDECEK

<!-- nav:bottom:start -->
---

| ← Önceki Bölüm | İçindekiler | Sonraki Bölüm → |
| :--- | :---: | ---: |
| [Bölüm 13: HTTP Boundary, DTO, Validasyon ve Hata Sözleşmeleri](java-chapter-13.md#top) | [Ders Notları](README.md#top) | |
<!-- nav:bottom:end -->
