# Alıştırma 00 *(SOLID İlkeleri ve Tasarım Kalıpları)*

Bu testin amacı **SOLID *(Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion)*** ilkeleri ile kurumsal projelerde sık kullanılan bazı tasarım kalıplarını *(Strategy, State, Chain of Responsibility, Decorator, Adapter, Command)* **Java** dili üzerinden, kod parçaları ve senaryolar yardımıyla değerlendirmektir. Testin sonunda cevap anahtarı yer almaktadır.

*Not: Sorular Java 21 *(JDK 21)* sürümü üzerinden hazırlanmıştır.*

---

## Soru 1

Geliştirilmekte olan bir abonelik platformunda yazılım geliştirici olarak görev alıyorsunuz. Müşteri modülünde yeni abonelerin JSON *(JavaScript Object Notation)* formatında gelen istekler üzerinden sisteme kaydedildiği bir senaryo söz konusu. Bu işi üstlenen **SubscriberController** sınıfının ilk sürümü aşağıdaki kod parçasında olduğu gibi yazılıyor.

```java
public class SubscriberController {
    private Database db = new Database();

    public String createSubscriber(String jsonValue) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Subscriber subscriber = mapper.readValue(jsonValue, Subscriber.class);
        if (!isValidSubscriber(subscriber)) {
            return "ERROR";
        }
        db.save(subscriber);
        return "SUCCESS";
    }

    private boolean isValidSubscriber(Subscriber subscriber) { /* trim + regex kontrolleri */ }
    private boolean isValidAlphaNumeric(String value) { /* ... */ }
    private boolean isValidEmail(String value) { /* ... */ }
}
```

Bu sınıf JSON dönüşümü, verinin temizlenmesi *(trim)*, doğrulama *(validation)* ve depolama *(persistence)* işlerini tek başına üstlenmektedir. Bu nedenle e-posta kuralında, JSON kütüphanesinde veya veritabanında yapılacak herhangi bir değişiklik bu sınıfın değiştirilmesini de gerektirir. Buna göre **Single Responsibility Principle *(SRP)*** prensibi açısından aşağıdaki düzenlemelerden hangisinin doğru olduğu söylenebilir?

- A) Doğrulama metotlarını **static** hale getirip bir **Utility** sınıfına taşımak, JSON dönüşümü ve veritabanı kaydını controller içinde bırakmak.
- B) **SubscriberController** sınıfını **abstract** yapıp JSON, doğrulama ve kayıt işlemlerini farklı alt sınıflarda **override** etmek.
- C) **SubscriberMapper**, **SubscriberNormalizer**, **SubscriberValidator** ve **SubscriberRepositoryService** gibi her biri tek bir işten sorumlu sınıflar oluşturmak ve controller'ın sadece akışı yönetmesini *(orchestration)* sağlamak.
- D) Tüm **private** metotları **public** yaparak sınıfın birim testlerle daha kolay test edilmesini sağlamak.

## Soru 2

Aşağıdaki doğrulama metodu bir önceki sorudaki controller sınıfından alınmıştır.

```java
private boolean isValidSubscriber(Subscriber subscriber) {
    if (!isExist(subscriber.getName())) {
        return false;
    }
    subscriber.setName(subscriber.getName().trim());
    if (!isValidAlphaNumeric(subscriber.getName())) {
        return false;
    }
    if (subscriber.getEmail() == null || subscriber.getEmail().trim().length() == 0) {
        return false;
    }
    subscriber.setEmail(subscriber.getEmail().trim());
    return isValidEmail(subscriber.getEmail());
}
```

Bu metodu tasarım açısından değerlendirdiğinizde dikkat çeken en kritik sorun aşağıdakilerden hangisidir?

- A) Metot adı doğrulama yaptığını söylese de **setName** ve **setEmail** çağrıları ile nesne verisini değiştirmektedir *(side effect)*. Verinin temizlenmesi *(normalization)* ve doğrulanması *(validation)* iki ayrı sorumluluktur ve doğrulama yapan kodun yan etkisinin olmaMAsı beklenir.
- B) **Pattern.compile** çağrısı metot her çalıştığında tekrarlandığı için kod derlenmez.
- C) Metot **private** tanımlandığı için **Subscriber** nesnesinin setter metotlarına erişemez.
- D) Java'da **boolean** dönen metotlar içinde birden fazla **return** ifadesi kullanılamaz.

## Soru 3

Farklı LLM *(Large Language Model)* sağlayıcılarına abone olan kullanıcıların harcadığı AI kredi harcamalarını hesaplayan bir faturalandırma servisi geliştirdiğimizi düşünelim. Bu servisin içeriği aşağıdaki gibidir. **ClaudeSubscriber**, **GrokSubscriber** ve **LlamaSubscriber** sınıfları arasında herhangi bir ortak üst tip *(abstraction)* bulunmaMAktadır.

```java
public class BillingService {
    public double calculateTotalCredit(List<Object> subscribers) {
        double total = 0;
        for (Object subscriber : subscribers) {
            total += calculateCredit(subscriber);
        }
        return total;
    }

    public double calculateCredit(Object subscriber) {
        if (subscriber instanceof ClaudeSubscriber claude) {
            return claude.calculateAiCredit();
        } else if (subscriber instanceof GrokSubscriber grok) {
            return grok.calculateAiCredit();
        } else if (subscriber instanceof LlamaSubscriber llama) {
            return llama.calculateAiCredit();
        }
        throw new IllegalArgumentException("Bilinmeyen abone türü: " + subscriber.getClass().getSimpleName());
    }
}
```

Bu kod parçası ile ilgili aşağıdaki görüşler ortaya atılmıştır;

- **I.** Sisteme **MistralSubscriber** gibi yeni bir abone türü eklendiğinde **BillingService** sınıfına yeni bir **else if** dalı eklemek gerekir. Bu durum **Open/Closed Principle *(OCP)*** ihlalidir.
- **II.** Yeni dalı eklemeyi unutmak derleme zamanında fark edilmez. Hata ancak çalışma zamanında **IllegalArgumentException** olarak ortaya çıkar.
- **III.** Abone sınıfları ortak bir soyut sınıftan *(abstract class)* türeseydi, servis `List<Subscriber>` üzerinde dolaşarak tip kontrolü yapmadan polimorfik çağrı yapabilirdi.
- **IV.** `List<Object>` kullanımı derleyiciye daha fazla bilgi verdiğinden tip güvenliğini *(type safety)* artırır.

Sizce hangi ifade/ifadeler doğrudur?

- A) Yalnız I
- B) I, II ve III
- C) II ve IV
- D) Hepsi

## Soru 4

Bir önceki sorudaki problemi çözmek için aşağıdaki gibi bir soyut sınıf tasarlanmıştır.

```java
public abstract class Subscriber {
    protected Long subscriberId;

    public final double calculateAiCredit() {
        long totalTokens = SessionHistory.getCurrentContexts(subscriberId).stream()
                .mapToLong(SessionHistory.Context::getContextTokens)
                .sum();
        return calculateCredit(totalTokens);
    }

    protected abstract double calculateCredit(long totalTokens);
}

public class ClaudeSubscriber extends Subscriber {
    @Override
    protected double calculateCredit(long totalTokens) {
        return totalTokens * 0.003;
    }
}
```

**calculateAiCredit** metodunda kullanılan **final** anahtar kelimesinin amacı ve bu yapının bilinen adı için aşağıdaki ifadelerden hangisi doğrudur?

- A) **final**, metodun dönüş değerinin bir kez hesaplandıktan sonra değişmeyeceğini belirtir. **Singleton** kalıbı kullanılmıştır.
- B) **final**, **Subscriber** sınıfından kalıtım yapılmasını engeller. **Factory Method** kalıbı kullanılmıştır.
- C) **final**, metodun sonucunu ilk çağrıdan sonra önbelleğe *(cache)* alır. **Decorator** kalıbı kullanılmıştır.
- D) **final**, alt sınıfların ortak akışı *(token toplama + hesaplama)* ezmesini *(override)* engeller. Değişen adım ise **protected abstract** metot ile alt sınıflara bırakılır. **Template Method** kalıbı kullanılmıştır.

## Soru 5

Üçüncü soruda aşağıdaki gibi bir söz dizimi *(syntax)* kullanılmaktadır.

```java
if (subscriber instanceof ClaudeSubscriber claude) {
    return claude.calculateAiCredit();
}
```

Bu söz dizimi ile ilgili aşağıdaki ifadeler ortaya atılmıştır:

- **I.** Tip kontrolü ve tip dönüşümü *(cast)* tek adımda yapılır. Ayrıca `(ClaudeSubscriber) subscriber` şeklinde bir dönüşüm yazmaya gerek kalmaz.
- **II.** **claude** değişkeni yalnızca koşulun **true** olduğu kapsamda *(scope)* kullanılabilir.
- **III.** Bu söz dizimi **Java 8** sürümünden beri dilde yer almaktadır.
- **IV.** **subscriber** değişkeni **null** ise bu ifade **NullPointerException** fırlatır.

Sizce hangi ifade/ifadeler doğrudur?

- A) I ve III
- B) Yalnız I
- C) I ve II
- D) Hepsi

## Soru 6

Token maliyeti hesaplayan bir hiyerarşide üst sınıf *(base class)* şu sözleşmeyi *(contract)* vaat etmektedir; *"Her zaman geçerli ve sıfırdan büyük veya eşit bir değer döner, asla exception fırlatmaz."* Bu taban sınıftan türeyen sınıflardan birisi de aşağıdaki gibidir.

```java
public class OfflineTokenCalculator extends TokenCalculator {
    private boolean isConnected = false;

    @Override
    public double calculateCost(long tokens) {
        if (!isConnected) {
            throw new UnsupportedOperationException(
                "Offline modu: Token hesaplama hizmeti kullanılamıyor.");
        }
        return 0;
    }
}
```

`TokenCalculator` türünden bir değişken bekleyen istemci kod, bu sınıfın bir örneği verildiğinde çalışma zamanında hata almaktadır. Sizce burada hangi ilke ihlal edilmiştir ve en uygun çözüm hangisidir?

- A) **Liskov Substitution Principle *(LSP)*** ihlal edilmiştir. Alt sınıf, temel sınıfın yerine kullanılamaMAktadır. Çözüm olarak bağlantı yokken de sözleşmeyi koruyan *(örneğin önbellekteki son bilinen oranla hesaplayan)* bir implementasyon yazılmalı, bağlantı yönetimi ise **Connectable** gibi ayrı bir rol arayüzüne alınmalıdır.
- B) **Interface Segregation Principle *(ISP)*** ihlal edilmiştir. Çözüm olarak **calculateCost** metodu iki farklı arayüze bölünmelidir.
- C) **Open/Closed Principle *(OCP)*** ihlal edilmiştir. Çözüm olarak temel sınıfa **if (calculator instanceof OfflineTokenCalculator)** kontrolü eklenmelidir.
- D) **Liskov Substitution Principle *(LSP)*** ihlal edilmiştir. Çözüm olarak exception fırlatmak yerine bağlantı yokken sessizce **0** döndürülmelidir.

## Soru 7

Aşağıdaki arayüzün sözleşmesi yorum satırlarında belirtilmiştir.

```java
/*
  Ön koşul  : tokens >= 0 beklenir. Negatif değerler 0 kabul edilir.
  Son koşul : Dönüş değeri her zaman >= 0'dır.
              Aynı girdi için her zaman aynı sonuç döner (deterministik).
  Exception : Hiçbir koşulda exception fırlatılmaz.
  Alt tipler bu sözleşmeyi zayıflatamaz, sadece daha güçlü garantiler ekleyebilir.
*/
public interface TokenCalculator {
    double calculateCost(long tokens);
}
```

Aşağıdaki implementasyonlardan hangisi **Liskov Substitution Principle** açısından bu sözleşmeye uygundur? *(RATE = 0.002 kabul ediniz)*

A)

```java
public double calculateCost(long tokens) {
    return tokens * RATE;
}
```

B)

```java
public double calculateCost(long tokens) {
    if (tokens > 1_000_000) {
        throw new IllegalArgumentException("Çok büyük talep");
    }
    return tokens <= 0 ? 0 : tokens * RATE;
}
```

C)

```java
public double calculateCost(long tokens) {
    return tokens <= 0 ? 0 : tokens * RATE * (0.9 + Math.random() * 0.2);
}
```

D)

```java
public double calculateCost(long tokens) {
    double commission = 1.10; // %10 platform komisyonu
    return tokens <= 0 ? 0 : tokens * RATE * commission;
}
```

## Soru 8

Bellek üzerinde *(in-memory)* çalışan bir veri erişim katmanında tüm entity türleri için aşağıdaki generic arayüz tanımlanmıştır. **Customer** sınıfının **title** alanı vardır ancak **Invoice** sınıfının böyle bir alanı yoktur.

```java
public interface PersistenceService<T extends Entity> {
    void save(T entity);
    void delete(T entity);
    T findById(Long id);
    List<T> findByTitle(String title);
}

public class InvoicePersistenceService implements PersistenceService<Invoice> {
    // save, delete, findById implementasyonları...

    @Override
    public List<Invoice> findByTitle(String title) {
        // Invoice nesnelerinin title alanı yok!
        return null;
    }
}
```

Bu tasarımdaki problemi **Interface Segregation Principle *(ISP)*** doğrultusunda çözmek için aşağıdakilerden hangisi tercih edilmelidir?

- A) **Invoice** sınıfına kullanılmayacak olsa da boş bir **title** alanı eklemek.
- B) **findByTitle** metodunu `TitleSearchable<T extends Entity>` isimli ayrı bir rol arayüzüne *(role interface)* taşımak; **CustomerPersistenceService** sınıfının hem **PersistenceService** hem de **TitleSearchable** arayüzlerini, **InvoicePersistenceService** sınıfının ise yalnızca **PersistenceService** arayüzünü uygulamasını sağlamak.
- C) **null** döndürmek yerine **UnsupportedOperationException** fırlatmak.
- D) **findByTitle** metodunu arayüz içinde `default` metot olarak tanımlayıp varsayılan olarak **null** döndürmesini sağlamak.

## Soru 9

Kodları aşağıda verilen DataInspectorService sınıfı veriyi **JSON** formatına çevirip bir dosyaya yazmak üzere kodlanmış writeData isimli bir fonksiyonellik sağlamaktadır.

```java
public class DataInspectorService {
    public void writeData(Data data, String destination) throws IOException {
        JsonFormatter jsonFormatter = new JsonFormatter();
        try (PrintWriter writer = new PrintWriter(new FileWriter(destination))) {
            writer.write(jsonFormatter.format(data));
        }
    }
}
```

İlerleyen zamanlarda **XML** ve **CSV** formatlarının da desteklenmesi, ayrıca çıktının dosya yerine konsola ya da belleğe yazılabilmesi de istenmektedir. **Dependency Inversion Principle *(DIP)*** açısından bu vakayı değerlendirdiğimizde, aşağıdaki tasarımlardan hangisi en uygun çözüm olur?

A)

```java
public interface Formatter {
    String format(Data data);
}

public class DataInspectorService {
    private final Formatter formatter;

    public DataInspectorService(Formatter formatter) {
        this.formatter = formatter;
    }

    public void writeData(Data data, Writer writer) throws IOException {
        writer.write(formatter.format(data));
        writer.flush();
    }
}
```

B)

```java
public class DataInspectorService {
    private final JsonFormatter formatter = new JsonFormatter();

    public void writeData(Data data, String destination) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destination))) {
            writer.write(formatter.format(data));
        }
    }
}
```

C)

```java
public class DataInspectorService {
    public void writeData(Data data, String destination, String format) throws IOException {
        String output = switch (format) {
            case "json" -> new JsonFormatter().format(data);
            case "xml"  -> new XmlFormatter().format(data);
            default     -> new CsvFormatter().format(data);
        };
        Files.writeString(Path.of(destination), output);
    }
}
```

D)

```java
public class DataInspectorService extends JsonFormatter {
    public void writeData(Data data, String destination) throws IOException {
        Files.writeString(Path.of(destination), format(data));
    }
}
```

## Soru 10

Dokuzuncu sorunun doğru cevabındaki tasarımın kullanıldığını ve **Formatter** arayüzünün `@FunctionalInterface` notasyonu *(annotation)* **içermediğini** varsayalım. **throws IOException** bildirimi olan bir test metodunda aşağıdaki kod parçası çalıştırılıyor olsun. **Data** sınıfının yapıcısı *(constructor)* `Data(int id, String content)` şeklindedir.

```java
Formatter formatter = d -> "id=" + d.getId();
DataInspectorService service = new DataInspectorService(formatter);

StringWriter out = new StringWriter();
service.writeData(new Data(7, "hello"), out);

System.out.println(out.toString().strip());
```

Sizce bu kodun çıktısı ne olur?

- A) Kod derlenmez zira **Formatter** arayüzünde `@FunctionalInterface` notasyonu olmadığından lambda ifadesi atanamaz.
- B) `id=7`
- C) `{ "id": 7, "content": "hello" }`
- D) Çalışma zamanında **IOException** fırlatılır. Çünkü yazılacak bir dosya yolu verilmemiştir.

## Soru 11

Bir CSV formatlayıcısı yazdığımızı düşünelim. Alanları bir separator ile birleştirmek için ilk etapta aşağıdaki gibi bir kod yazdığımızı varsayalım.

```java
int id = 42;
char separator = ',';
String line = id + separator + "hello";
System.out.println(line);
```

Sizce bu kodun çıktısı ne olur? *(İpucu: ',' karakterinin Unicode/ASCII değeri 44'tür.)*

- A) `42,hello`
- B) `42hello`
- C) Kod derlenmez, **int** ile **char** toplanamaz.
- D) `86hello`

## Soru 12

Bir e-ticaret sitesinde kargo ücreti teslimat tipine *(STANDARD, EXPRESS, PICKUP_POINT, SAME_DAY)* göre hesaplanmaktadır. İlk sürümde tüm kurallar tek bir metot içinde **switch** ve iç içe **if** blokları ile yazılmış, statik kod analizi aracı yüksek **Cognitive Complexity** uyarısı vermiştir. Ekip, her teslimat tipinin fiyatlandırma kuralını ayrı bir sınıfa taşıyarak aşağıdaki tasarıma karar vermiştir.

```text
┌─────────────────────────────────┐      use     ┌──────────────── [ ? ] ─────────────────┐
│ ShippingCostCalculator          │─────────────>│                                        │
│ - strategies:                   │              │                                        │
│   EnumMap<DeliveryType, ?>      │              │                                        │
│ + calculate(shipment, type)     │              └───────────────────▲────────────────────┘
└─────────────────────────────────┘                                  │ implements
                    ┌───────────────────┬─────────────────────┬──────┴──────────────┐
                    │                   │                     │                     │
          ┌─────────┴────────┐ ┌────────┴────────┐ ┌──────────┴──────────┐ ┌────────┴────────┐
          │ StandardShipping │ │ ExpressShipping │ │ PickupPointShipping │ │ SameDayShipping │
          └──────────────────┘ └─────────────────┘ └─────────────────────┘ └─────────────────┘
```

Aşağıdakilerden hangisi, diyagramda **[ ? ]** ile gösterilen eksik parçanın yerine gelebilir?

A)

```java
public abstract class ShippingStrategy {
    public abstract double calculate(Shipment shipment, DeliveryType type);
    // her alt sınıf içinde type değerine göre switch yapılır
}
```

B)

```java
public enum ShippingStrategy {
    STANDARD, EXPRESS, PICKUP_POINT, SAME_DAY
}
```

C)

```java
@FunctionalInterface
public interface ShippingStrategy {
    double calculate(Shipment shipment);
}
```

D)

```java
public interface ShippingStrategy {
    double calculate(Shipment shipment);
    DeliveryType nextDeliveryType(); // bir sonraki teslimat tipine kendisi karar verir
}
```

## Soru 13

Bir önceki sorudaki tasarımın **context** sınıfı aşağıdaki gibidir.

```java
public class ShippingCostCalculator {
    private final Map<DeliveryType, ShippingStrategy> strategies = new EnumMap<>(DeliveryType.class);

    public ShippingCostCalculator() {
        strategies.put(DeliveryType.STANDARD, new StandardShipping());
        strategies.put(DeliveryType.EXPRESS, new ExpressShipping());
        strategies.put(DeliveryType.PICKUP_POINT, new PickupPointShipping());
        strategies.put(DeliveryType.SAME_DAY, new SameDayShipping());
    }

    public void register(DeliveryType type, ShippingStrategy strategy) {
        strategies.put(type, strategy);
    }

    public double calculate(Shipment shipment, DeliveryType type) {
        ShippingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Bilinmeyen teslimat tipi: " + type);
        }
        return Math.round(strategy.calculate(shipment) * 100) / 100.0;
    }
}
```

Kampanya döneminde ekspres kargonun ücretsiz olması istendiği için aşağıdaki satır çalıştırılmıştır.

```java
calculator.register(DeliveryType.EXPRESS, shipment -> 0.0);
```

Eklenen bu yeni kod parçası ile ilgili aşağıdaki ifadeler ortaya atılmıştır.

- **I.** **ShippingStrategy** tek bir soyut metoda sahip olduğundan, yeni bir sınıf yazmadan lambda ifadesi ile strateji verilebilir.
- **II.** **EnumMap** yalnızca belirtilen enum türünün sabitlerini anahtar olarak kabul eden, enum için optimize edilmiş bir **Map** implementasyonudur.
- **III.** **register** çağrısından sonra **EXPRESS** tipi için **calculate** metodu **0.0** döner. Çünkü mevcut strateji yenisi ile değiştirilmiştir.
- **IV.** İleride **NIGHT_DELIVERY** gibi yeni bir teslimat tipi eklemek için **calculate** metoduna yeni bir **case** eklenmesi zorunludur.

Sizce bu ifadelerden hangisi/hangileri doğrudur?

- A) I, II ve III
- B) Hepsi
- C) I ve IV
- D) Yalnız II

## Soru 14

Bir e-ticaret platformunda siparişler **NEW -> PAID -> SHIPPED -> DELIVERED** şeklinde ilerlemekte, belirli aşamalarda ise **CANCELLED** durumuna geçebilmektedir. Mevcut **Order** sınıfında **pay()**, **ship()**, **deliver()** ve **cancel()** metotlarının her biri `switch (status)` bloğu ile başlamakta ve aynı durum kontrolleri tüm metotlarda tekrar etmektedir. Ayrıca *"ödenmiş sipariş iptal edilirse iade başlatılır"*, *"kargodaki sipariş iptal edilemez"* gibi kurallar bu bloklara dağılmış durumdadır. Amaç her durumun kendi davranışını bilmesi ve bir sonraki geçişe **durumun kendisinin** karar vermesidir.

Sizce bu senaryo için en uygun tasarım kalıbı hangisidir?

- A) **Strategy** - Çünkü hangi algoritmanın çalışacağını istemci seçer ve stratejiler birbirinden habersizdir.
- B) **State** - Çünkü nesnenin davranışı içinde bulunduğu duruma göre değişmekte, her durum ayrı bir sınıf olarak modellenmekte ve geçişlere durum nesneleri karar vermektedir.
- C) **Command** - Çünkü her sipariş işlemi geri alınabilir *(undo)* bir komut olarak saklanmalıdır.
- D) **Adapter** - Çünkü sipariş durumları dış bir sistemin formatına dönüştürülmelidir.

## Soru 15

Bir önceki senaryo **State** kalıbı ile aşağıdaki gibi yeniden tasarlanmıştır *(kodun sadece ilgili kısımları verilmiştir)*.

```java
public interface OrderState {
    String name();
    default void pay(Order order)     { throw invalid("ödenemez"); }
    default void ship(Order order)    { throw invalid("kargoya verilemez"); }
    default void deliver(Order order) { throw invalid("teslim edilemez"); }
    default void cancel(Order order)  { throw invalid("iptal edilemez"); }

    private IllegalStateException invalid(String action) {
        return new IllegalStateException(name() + " durumundaki sipariş " + action);
    }
}

final class PaidState implements OrderState {
    public String name() { return "PAID"; }
    public void ship(Order order)   { order.transitionTo(new ShippedState(), "Kargoya verildi"); }
    public void cancel(Order order) { order.transitionTo(new CancelledState(), "İptal edildi, ödeme iadesi başlatıldı"); }
}

final class ShippedState implements OrderState {
    public String name() { return "SHIPPED"; }
    public void deliver(Order order) { order.transitionTo(new DeliveredState(), "Teslim edildi"); }
    public void cancel(Order order) {
        throw new IllegalStateException("Kargodaki sipariş iptal edilemez, iade süreci başlatılmalı");
    }
}
```

Aşağıdaki kod parçası çalıştırıldığında ekran çıktısı ne olur? *(NewState sınıfının pay metodu siparişi PaidState durumuna geçirmektedir.)*

```java
Order order = new Order("SIP-1001");
order.pay();
order.ship();
try {
    order.cancel();
} catch (IllegalStateException e) {
    System.out.println(e.getMessage());
}
System.out.println(order.getStatus());
```

- A)
  ```text
  İptal edildi, ödeme iadesi başlatıldı
  CANCELLED
  ```
- B) Kod derlenmez, çünkü **transitionTo** metodu **package-private** tanımlanmıştır.
- C)
  ```text
  Kargodaki sipariş iptal edilemez, iade süreci başlatılmalı
  SHIPPED
  ```
- D)
  ```text
  SHIPPED durumundaki sipariş iptal edilemez
  SHIPPED
  ```

## Soru 16

Bir bankanın havale/EFT modülünde transfer talebi sırasıyla şu kontrollerden geçmektedir; *IBAN formatı -> alıcının kara listede olup olmadığı -> bakiye -> günlük limit -> 50.000 birim üzeri işlemlerde açıklama zorunluluğu*. İlk sürümde bu kontroller dokuz seviye iç içe **if** bloğundan oluşan *"arrow code"* biçiminde yazılmıştır. Kontrollerden herhangi biri başarısız olduğunda akış kesilmeli, başarılıysa talep bir sonraki kontrole devredilmelidir. Bu nedenle kod tekrardan gözden geçirilmiş ve **Chain of Responsibility** kalıbı tercih edilerek ve aşağıdaki soyut sınıf *(abstract class)* yazılmıştır.

```java
public abstract class TransferCheck {
    private TransferCheck next;

    public TransferCheck linkWith(TransferCheck next) {
        this.next = next;
        return next;
    }

    public final TransferResult check(TransferRequest request) {
        Optional<String> rejection = verify(request);
        if (rejection.isPresent()) {
            return TransferResult.reject(rejection.get());
        }
        if (next == null) {
            return TransferResult.approve();
        }
        ...................................
    }

    protected abstract Optional<String> verify(TransferRequest request);
}
```

Sizce boş bırakılan satıra *(...)* aşağıdaki ifadelerden hangisi gelmelidir?

- A) `return next.verify(request);`
- B) `return TransferResult.approve();`
- C) `next = null; return check(request);`
- D) `return next.check(request);`

## Soru 17

Döviz kuru getiren bir servise önbellekleme *(cache)*, loglama ve geçici hatalarda yeniden deneme *(retry)* yetenekleri **Decorator** kalıbı ile eklenmiştir. Tüm sınıflar **ExchangeRateProvider** arayüzünü uygulamaktadır. Davranışlar şöyledir;

- **RemoteRateClient(2)**: Uzak servisi temsil eder, ilk **2** çağrıda zaman aşımı *(IllegalStateException)* fırlatır, sonrakilerde kur değerini döner. **getCallCount()** uzak servise kaç kez gidildiği bilgisini verir.
- **RetryingRateProvider(delegate, 3)**: **IllegalStateException** alındığında en fazla **3** deneme yapar.
- **CachingRateProvider(delegate)**: Aynı döviz kuru çifti için sarmaladığı nesneye yalnızca bir kez gider *(computeIfAbsent)*.
- **LoggingRateProvider(delegate, log)**: Her çağrıda istek ve başarılı sonuç için birer log satırı yazar.

```java
RemoteRateClient client = new RemoteRateClient(2);
List<String> log = new ArrayList<>();

ExchangeRateProvider provider =
        new LoggingRateProvider(
                new CachingRateProvider(
                        new RetryingRateProvider(
                                new RemoteExchangeRateProvider(client), 3)),
                log::add);

provider.getRate("USD/TRY");
provider.getRate("USD/TRY");

System.out.println(client.getCallCount() + " / " + log.size());
```

Sizce bu kodun çalışma zamanı çıktısı ne olur?

- A) `3 / 4`
- B) `6 / 4`
- C) `3 / 2`
- D) `1 / 4`

## Soru 18

Bir e-ticaret uygulaması iki farklı sanal POS sağlayıcısı *(provider)* ile çalışmaktadır. Sağlayıcıların kütüphaneleri bize ait değildir ve değiştirilemez.

- **PayFastClient**: Tutarı kuruş cinsinden **long** olarak bekler *(149.90 TL → 14990)* ve sonucu sayısal bir kod ile döner *(0 = başarılı)*.
- **LegacyBankPos**: XML metni ile çalışır, tutarı metinsel bilgi olarak ve virgüllü bekler *("1250,50")*, cevap kodu **"00"** ise başarılıdır.

İş akışının *(CheckoutService)* bu detayları bilmeMEsi ve yalnızca kendi dilinde tasarlanmış bir arayüzle *(TL cinsinden tutar, PaymentResult dönüşü)* konuşması hedeflenmektedir. Aşağıdaki çizelgede yer alan tasarım ile ilerlenmesi kararlaştırılmıştır.

```text
┌──────────────────┐      use     ┌────────────────────────────────────┐
│ CheckoutService  │─────────────>│ <<interface>> PaymentGateway       │
└──────────────────┘              │ + charge(orderNo, double amount)   │
                                  │   : PaymentResult                  │
                                  └─────────────────▲──────────────────┘
                                                    │ implements
                          ┌─────────────────────────┴──────────────────────┐
                          │                                                │
                 ┌────────┴─────────┐                          ┌───────────┴───────────┐
                 │      [ ? ]       │                          │ LegacyBankPosAdapter  │
                 └────────┬─────────┘                          └───────────┬───────────┘
                          │ has-a                                          │ has-a
                 ┌────────▼──────────────────┐                 ┌───────────▼───────────┐
                 │ PayFastClient             │                 │ LegacyBankPos         │
                 │ + makePayment(ref,        │                 │ + process(xml)        │
                 │   long kurus, currency)   │                 └───────────────────────┘
                 └───────────────────────────┘
```

Sizce **[ ? ]** ile gösterilen eksik parça için aşağıdakilerden hangisi kullanılmalıdır?

A)

```java
public class PayFastAdapter extends PayFastClient {
    // PaymentGateway uygulanmaz, CheckoutService doğrudan makePayment çağırır
}
```

B)

```java
public class PayFastAdapter implements PaymentGateway {
    private final PayFastClient client;

    public PayFastAdapter(PayFastClient client) { this.client = client; }

    @Override
    public PaymentResult charge(String orderNo, double amount) {
        PayFastResponse response = client.makePayment(orderNo, Math.round(amount * 100), "TRY");
        return response.code() == 0
                ? PaymentResult.ok(response.transactionRef())
                : PaymentResult.failed("Ödeme alınamadı (PayFast kodu: " + response.code() + ")");
    }
}
```

C)

```java
public class CheckoutService {
    public PaymentResult pay(String provider, String orderNo, double amount) {
        if ("PAYFAST".equals(provider)) { /* kuruş dönüşümü + kod kontrolü */ }
        else if ("BANK".equals(provider)) { /* XML oluşturma + "00" kontrolü */ }
        return PaymentResult.failed("Bilinmeyen sağlayıcı");
    }
}
```

D)

```java
public interface PaymentGateway {
    // Hedef arayüz PayFast'e uyacak şekilde değiştirilir
    PayFastResponse charge(String orderNo, long amountInKurus, String currencyCode);
}
```

## Soru 19

Sıra tabanlı bir strateji oyununda hamlelerin geri alınması *(undo)* ve yinelenmesi *(redo)* için **Command** tasarım kalıbı kullanılmıştır. **CommandHistory** sınıfı aşağıdaki gibi tasarlanmıştır.

```java
public class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }
}
```

**MoveUnitCommand(board, unit, dx, dy)** birimi verilen miktar kadar hareket ettirir, **undo** çağrıldığında ise önceki konumuna geri götürür. Örnek bir kullanım senaryosu aşağıdaki gibidir.

```java
GameBoard board = new GameBoard();
board.addUnit("Okçu", new Position(0, 0), 100);
CommandHistory history = new CommandHistory();

history.execute(new MoveUnitCommand(board, "Okçu", 1, 0));
history.execute(new MoveUnitCommand(board, "Okçu", 0, 2));
history.undo();
history.execute(new MoveUnitCommand(board, "Okçu", 3, 0));
boolean result = history.redo();

System.out.println(board.positionOf("Okçu") + " " + result);
```

Yukarıdaki kod çalıştırıldığında *Okçu* biriminin son konumu ve **result** değişkeninin değeri ne olur?

- A) `Position[x=1, y=2] true`
- B) `Position[x=1, y=2] false`
- C) `Position[x=4, y=2] true`
- D) `Position[x=4, y=0] false`

## Soru 20

Kod incelemelerinde *(code review)* karşılaşılan belirtilerden hangi tasarım kalıbına bakılması gerektiğini bilmek, doğru refactoring kararını vermeyi kolaylaştırır. Aşağıdaki tabloda kodda görülen belirtiler ve ilgili tasarım kalıpları yer almaktadır.

| | **Kodda görülen belirti** | | **Harf** | **Tasarım kalıbı** |
| --- | --- | --- | --- | --- |
| 1 | Bir **state alanına** göre dallanan ve birden çok metotta tekrar eden `switch` blokları | | a | **Chain of Responsibility** |
| 2 | Asıl işin etrafına eklenen `cache/log/retry` gibi yetenekler, metotlara eklenen `boolean` türü parametreler | | b | **Command** |
| 3 | Geri alma *(undo)*, yineleme *(redo)*, kuyruğa alma *(queue)* veya işlemleri tekrar oynatma *(replay)* ihtiyacı | | c | **Decorator** |
| 4 | Sırayla çalışan ve herhangi biri akışı kesebilen kontroller, iç içe `if` blokları | | d | **State** |

Sizce doğru eşleştirme hangisidir?

- A) 1-a, 2-c, 3-b, 4-d
- B) 1-d, 2-b, 3-c, 4-a
- C) 1-d, 2-c, 3-b, 4-a
- D) 1-c, 2-d, 3-a, 4-b

---

## Cevap Anahtarı

| Soru No | Doğru Cevap |
| ------- | ----------- |
| 1 | C |
| 2 | A |
| 3 | B |
| 4 | D |
| 5 | C |
| 6 | A |
| 7 | D |
| 8 | B |
| 9 | A |
| 10 | B |
| 11 | D |
| 12 | C |
| 13 | A |
| 14 | B |
| 15 | C |
| 16 | D |
| 17 | A |
| 18 | B |
| 19 | D |
| 20 | C |
