# Alıştırma 01 *(Java Patterns and Practices)*

Bu testin amacı Java ile business nesne tasarımı *(value object, entity, aggregate)*, parasal hesaplamalar, Optional ve Stream API, Specification deseni, domain event'ler, hata sözleşmeleri, transaction yönetimi ve depolama *(persistence)* konularını kod parçaları ve senaryolar yardımıyla değerlendirmektir. Testin sonunda cevap anahtarı yer almaktadır.

*Not: Sorular Java 21 *(JDK 21)* sürümü üzerinden hazırlanmıştır.*

---

## Soru 1

Uygulama geliştirme yaşam döngüsünde, branch stratejisi olarak **Gitflow** kullanılıyor. Üretim ortamında **1.3** sürümü çalışıyor. **1.4** sürümü için açılan `release/1.4` dalı ise şu anda kullanıcı kabul testinde *(UAT - User Acceptance Test)*. Bu sırada üretim ortamında müşterilerin ödeme yapmasını engelleyen kritik bir hata bulunuyor ve hemen bir **hotfix** çıkılması gerekiyor. Ayrıca hatanın **1.4** sürümüyle birlikte tekrar üretime dönmemesi de isteniyor. Sizce hotfix dalı nereden açılmalı ve nereye merge edilmelidir?

- A) Hotfix, `dev` dalından açılır ve sadece `dev` dalına merge edilir. Düzeltme bir sonraki release ile birlikte üretime çıkar.
- B) Hotfix, `main` dalından açılır. Önce `main` dalına merge edilip yeni bir sürüm etiketi *(tag)* atılır ardından açık olan `release/1.4` dalına merge edilir. Release kapatıldığında düzeltme `dev` dalına da taşınmış olur.
- C) Hotfix dalı `release/1.4` dalından açılır ve sadece `release/1.4` dalına merge edilir. Böylece üretimdeki **1.3** sürümüne dokunulmamış olur.
- D) Hotfix `main` dalından açılır, `main` ve `dev` dallarına merge edilir. Açık olan `release/1.4` dalına ise dokunulmaz.

## Soru 2

Uygulamada kullanılan **Northwind** veritabanındaki **Products** tablosunun kod tarafındaki karşılığı olarak aşağıdaki gibi bir sınıf tasarlanmıştır.

```java
public class Product {
    private int productId;
    private String productName;
    private short unitsInStock;

    public short getUnitsInStock() {
        return unitsInStock;
    }

    public void setUnitsInStock(short unitsInStock) {
        this.unitsInStock = unitsInStock;
    }
    // Diğer getter/setter metotları...
}
```

Depo görevlisinin girdiği **40_000** adetlik stok bilgisi aşağıdaki kod ile nesneye nesne örneğine aktarılmaktadır.

```java
Product product = new Product();
product.setUnitsInStock((short) 40000);
System.out.println(product.getUnitsInStock());
```

Sizce bu kodun ekran çıktısı ne olur? *(İpucu: short türü 16 bitlik işaretli bir tamsayıdır ve -32768 ile 32767 arasındaki değerleri tutabilir.)*

- A) `40000`
- B) Kod derlenmez, çünkü 40000 değeri **short** türüne sığmaz.
- C) Çalışma zamanında **ArithmeticException** fırlatılır.
- D) `-25536`

## Soru 3

Bir e-ticaret uygulamasında müşteri sepetini temsil etmek için iki farklı **record** tanımı yapılmıştır.

```java
record Basket(String owner, List<String> items) { }

record SafeBasket(String owner, List<String> items) {
    SafeBasket {
        items = List.copyOf(items);
    }
}
```

Bu iki record tanımının örnek kullanımları şöyledir;

```java
List<String> list = new ArrayList<>(List.of("Chai"));
Basket basket = new Basket("VINET", list);
SafeBasket safeBasket = new SafeBasket("VINET", list);

list.add("Chang");
```

Bu kod parçası ile ilgili aşağıdaki ifadeler ortaya atılmıştır.

- **I.** `basket.items().size()` ifadesi **1** değerini döner.
- **II.** `safeBasket.items().size()` ifadesi **1** değerini döner.
- **III.** `safeBasket.items().add("Ikura")` çağrısı **UnsupportedOperationException** fırlatır.
- **IV.** Record'lar derin *(deep)* değiştirilemez olduğu için her iki record'un da içeriği **1** eleman olarak kalır.

Sizce hangi ifade/ifadeler doğrudur?

- A) I ve IV
- B) Yalnız II
- C) II ve III
- D) Hepsi

## Soru 4

Bir siparişin kalemleri aşağıdaki record türünden veri yapısı ile temsil edilmektedir. İndirim oranı veritabanında `NUMERIC(3, 2)` tipindeki bir kolonda tutulmakta ve okunduğunda her zaman iki ondalık basamakla gelmektedir.

```java
record OrderLine(int productId, BigDecimal discount) { }
```

Bir sözleşme testinde *(contract test)* kaydedilen kalem ile veritabanından okunan kalem aşağıdaki kod parçasında olduğu gibi karşılaştırılmaktadır.

```java
OrderLine saved = new OrderLine(42, new BigDecimal("0.1"));
OrderLine loaded = new OrderLine(42, new BigDecimal("0.10"));

System.out.println(saved.equals(loaded) + " "
        + (saved.discount().compareTo(loaded.discount()) == 0) + " "
        + new HashSet<>(List.of(saved, loaded)).size());
```

Sizce bu kodun ekran çıktısı ne olur?

- A) `false true 2`
- B) `true true 1`
- C) `false false 2`
- D) `true false 1`

## Soru 5

Parasal değerleri temsil eden **Money** record'unun ilgili kısımları aşağıdaki gibidir. Tutar her zaman para biriminin ondalık basamak sayısına *(TRY için 2)* yuvarlanmaktadır.

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money tl(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("TRY"));
    }

    public Money discountedBy(BigDecimal rate) {
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discount rate must be within [0, 1]: " + rate);
        }
        return new Money(amount.multiply(BigDecimal.ONE.subtract(rate)), currency);
    }
}
```

Kampanya modülünde aşağıdaki gibi satır yer almaktadır.

```java
Money total = Money.tl("100").discountedBy(new BigDecimal(0.8));
```

Bu kod ile ilgili aşağıdaki ifadeler ortaya atılmıştır.

- **I.** `new BigDecimal(0.8)` ifadesi **0.8** değerini değil, double türünün ikilik tabandaki yaklaşık değerini *(0.8000000000000000444...)* tam olarak taşır.
- **II.** `BigDecimal.valueOf(0.8)` ifadesi `Double.toString` kullandığı için **0.8** değerini üretir.
- **III.** Bu satır toplam tutara **%20** indirim uygular.
- **IV.** **total** değişkeninin değeri **20.00 TRY** olur.

Sizce hangi ifade/ifadeler doğrudur?

- A) I ve III
- B) I, II ve IV
- C) Yalnız III
- D) Hepsi

## Soru 6

Aşağıda parasal değerleri temsil eden bir **Money** record'u yer almaktadır.

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money tl(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("TRY"));
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public Money discountedBy(BigDecimal rate) {
        return new Money(amount.multiply(BigDecimal.ONE.subtract(rate)), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
```

Buna göre aşağıdaki kod parçası çalıştırıldığında ekran çıktısı ne olur? *(Currency.getDefaultFractionDigits() değeri TRY için 2, Japon Yeni (JPY) için 0'dır.)*

```java
System.out.println(Money.tl("24.6").times(3));
System.out.println(Money.tl("1000").discountedBy(new BigDecimal("0.15")));
System.out.println(new Money(new BigDecimal("10.5"), Currency.getInstance("JPY")));
```

- A)
  ```text
  73.8 TRY
  850 TRY
  10.5 JPY
  ```
- B)
  ```text
  73.80 TRY
  850.00 TRY
  10.50 JPY
  ```
- C)
  ```text
  73.80 TRY
  850.00 TRY
  11 JPY
  ```
- D)
  ```text
  73.80 TRY
  150.00 TRY
  11 JPY
  ```

## Soru 7

Satış modülünde sipariş durumları arasındaki geçişleri merkezi bir yerde tutmak için bir **enum** tasarlanıyor. İzin verilen geçişlerin enum sabitinin yapıcı metodu *(constructor)* üzerinden verilmesi tercih ediliyor.

```java
public enum OrderStatus {
    DRAFT(EnumSet.of(CONFIRMED, CANCELLED)),
    CONFIRMED(EnumSet.of(SHIPPED, CANCELLED)),
    SHIPPED(EnumSet.noneOf(OrderStatus.class)),
    CANCELLED(EnumSet.noneOf(OrderStatus.class));

    private final Set<OrderStatus> allowedTransitions;

    OrderStatus(Set<OrderStatus> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions.contains(target);
    }
}
```

Sizce bu kod ile ilgili olarak aşağıdaki ifadelerden hangisi doğrudur?

- A) Kod derlenmez. **DRAFT** sabiti, henüz tanımlanmamış olan **CONFIRMED** ve **CANCELLED** sabitlerine başvurduğu için derleyici *illegal forward reference* hatası verir. Geçişler, tüm sabitler oluşturulduktan sonra çalışan bir `static` blokta tanımlanabilir.
- B) Kod derlenir ve sorunsuz çalışır. `DRAFT.canTransitionTo(CONFIRMED)` çağrısı **true** döner.
- C) Kod derlenir ancak **CONFIRMED** henüz oluşturulmadığı için `EnumSet.of` içine **null** değerler eklenir ve `canTransitionTo` çağrısı her zaman **false** döner.
- D) Kod derlenir ancak enum ilk kez kullanıldığında **ExceptionInInitializerError** fırlatılır.

## Soru 8

Northwind sistemindeki müşteri kimlikleri *(Identity)* beş harften oluşmaktadır. Geçerli bir kimlik bilgisinden söz edebilmek için de uyulması gereken bir kural vardır. Bu kural aşağıda tasarımı görülüne bir **typed identity** türü ile korunmaktadır.

```java
public record CustomerId(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{5}$");

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        value = value.strip().toUpperCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("customerId must be exactly five letters (A-Z): " + value);
        }
    }
}
```

`new CustomerId("vinet")` çağrısı geliştiricinin **en-US** bölge ayarlı bilgisayarında sorunsuz çalışmakta ve **VINET** değerini üretmektedir. Ancak aynı kod, varsayılan bölge ayarı **tr-TR** olan sunucuda **IllegalArgumentException** fırlatmaktadır. Sizce bu farklı davranışın sebebi nedir?

- A) `strip()` metodu Türkçe bölge ayarında baştaki ve sondaki boşlukları silmez.
- B) Düzenli ifadeler *(regex)* Türkçe bölge ayarında büyük/küçük harf duyarsız çalıştığı için desen eşleşmez.
- C) Record'ların compact constructor'ında parametreye yeniden değer atanamadığı için **value** değişmeden kalır.
- D) Parametresiz `toUpperCase()` varsayılan bölge ayarını kullanır. Türkçede **i** harfinin büyüğü **İ** *(U+0130)* olduğu için değer **VİNET** olur ve `[A-Z]` desenine uymaz. Bu yüzden `toUpperCase(Locale.ROOT)` kullanılmalıdır.

## Soru 9

Aşağıdaki kod parçasında aynı liste üzerinde iki ayrı stream akışı tanımlanmaktadır.

```java
List<String> names = List.of("Chai", "Chang", "Aniseed Syrup");

// Akış 1
names.stream()
        .peek(System.out::println)
        .filter(n -> n.length() > 4);

// Akış 2
List<Integer> lengths = names.stream()
        .filter(n -> n.length() > 4)
        .map(String::length)
        .toList();
```

Bu kod parçası ile ilgili aşağıdaki ifadeler ortaya atılmıştır.

- **I.** Akış 1'de bir terminal işlem *(terminal operation)* bulunmadığı için ara işlemler *(peek, filter)* hiç çalıştırılmaz.
- **II.** Akış 2, `toList()` terminal işlemi ile tüketilir.
- **III.** **lengths** listesinin içeriği `[5, 13]` olur.
- **IV.** Akış 1'deki `peek` çağrısı listedeki üç ismi de ekrana yazdırır.

Sizce hangi ifade/ifadeler doğrudur?

- A) I ve IV
- B) I, II ve III
- C) II ve IV
- D) Hepsi

## Soru 10

Bir müşteri portalında, oturum açan kullanıcının adı bulunamazsa varsayılan bir isim kullanılmaktadır. Varsayılan isim veritabanında bir parametre tablosundan gelmektedir ve bu okuma *(read)* maliyetli bir işlemdir. *(Bu durum `loadDefault()` metodunda aşağıdaki gibi simüle edilmektedir)*

```java
static String loadDefault() {
    System.out.println("DB call");
    return "Guest";
}

public static void main(String[] args) {
    Optional<String> name = Optional.of("Maria");

    String a = name.orElse(loadDefault());
    String b = name.orElseGet(() -> loadDefault());

    System.out.println(a + " " + b);
}
```

Sizce ana programın çalışma zamanı çıktısı şıklardan hangisidir?

- A)
  ```text
  Maria Maria
  ```
- B)
  ```text
  DB call
  DB call
  Maria Maria
  ```
- C)
  ```text
  DB call
  Maria Maria
  ```
- D)
  ```text
  DB call
  Guest Maria
  ```

## Soru 11

İş kurallarını nesne haline getirmek için kullanılan pratiklerden birisi de **Specification** desenidir. Bu desene göre örnek bir sözleşme tanımı aşağıdaki gibidir.

```java
@FunctionalInterface
public interface Specification<T> {

    boolean isSatisfiedBy(T candidate);

    default Specification<T> and(Specification<T> other) {
        return candidate -> isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    default Specification<T> or(Specification<T> other) {
        return candidate -> isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    default Specification<T> not() {
        return candidate -> !isSatisfiedBy(candidate);
    }
}
```

Generic olarak tanımlanmış sözleşmenin örnek uygulanışı aşağıdaki kod parçasında görüldüğü gibidir. Sizce bu kod çalıştırıldığında ekran çıktısı ne olur?

```java
Specification<Integer> even = n -> n % 2 == 0;
Specification<Integer> big = n -> n > 10;

Specification<Integer> spec = even.and(big.not()).or(n -> n == 15);

System.out.println(List.of(4, 12, 15, 7, 8).stream()
        .filter(spec::isSatisfiedBy)
        .toList());
```

- A) `[4, 15, 8]`
- B) `[4, 8]`
- C) `[4, 12, 15, 8]`
- D) `[15]`

## Soru 12

Bir satış sisteminde **Order** ve **Customer** iki ayrı aggregate root olarak tasarlanmıştır. **Order**, müşteriyi bir **Customer** nesnesi olarak değil, yalnızca **CustomerId** değeri ile tutmaktadır. Geliştirme döngüsünün ilerleyen günlerinde sisteme yeni bir iş kuralı gelir; *"Sipariş onaylandığında müşterinin kullanılabilir kredi limitinden sipariş tutarı kadar düşülmelidir."* Ekip, **Vaughn Vernon**'un *"bir transaction içinde yalnızca bir aggregate değiştirilmelidir"* önerisine uymak ister. Kredi güncellemesinin sipariş onayından kısa bir gecikmeyle tamamlanması iş birimi tarafından kabul edilmektedir.

Sizce bu şartlarda söz konusu iş kuralı için en uygun tasarım hangisidir?

- A) **Order** sınıfına bir **Customer** alanı eklemek, **confirm()** metodunun içinde `customer.decreaseCredit(total())` çağırmak ve iki nesneyi aynı transaction içinde kaydetmek.
- B) **Order** sınıfının yapıcısına bir **CustomerRepository** enjekte etmek ve **confirm()** metodu içinde müşteriyi okuyup güncellemek.
- C) Kredi limitinin düşülmesini tamamen veritabanındaki bir trigger'a bırakmak ve domain modelinde bu kurala hiç yer vermemek.
- D) Sipariş onaylandığında **OrderConfirmed** gibi bir domain event yayınlamak ve bu olayı dinleyen bir bileşenin **Customer** aggregate'ini ayrı bir işlemde güncellemesini sağlamak *(eventual consistency)*.

## Soru 13

Sistemde siparişlerle ilgili hizmet veren **OrderService** sınıfının **ship** metodu kargo tarihini aşağıdaki gibi belirlemektedir.

```java
public OrderSummary ship(int orderId) {
    return change(orderId, order -> order.ship(LocalDate.now()));
}
```

Sizin şu senaryo için otomatik bir test yazmanız beklenmektedir: *"Sipariş bugün veriliyor, altı gün sonra kargolanıyor ve kargo tarihi sipariş tarihinden altı gün sonrası olarak kaydediliyor."* Testin her gün, her makinede aynı sonucu vermesi gerekiyor.

Bu test şartını sağlamak için şıklardaki hangi yaklaşımı tercih edersiniz?

- A) `LocalDate.now()` kullanımını değiştirmeden testte beklenen değeri `LocalDate.now().plusDays(6)` olarak hesaplamak.
- B) Servise yapıcı metot üzerinden bir `java.time.Clock` enjekte etmek ve `LocalDate.now(clock)` kullanmak. Testte ise zamanı test ilerletene kadar sabit tutan *(örneğin `advance(Duration)` metodu olan)* bir **Clock** türevi vermek.
- C) Servise `Clock.systemDefaultZone()` enjekte etmek. Böylece sistem saati değiştirilerek test yapılabilir.
- D) Servise tek bir `Clock.fixed(...)` örneği enjekte etmek. Sabit saat, aynı test içinde hem sipariş gününü hem de altı gün sonrasını üretebilir.

## Soru 14

Bir sipariş servisinde kargolama işlemi sırasıyla şu adımlardan oluşmaktadır; önce sipariş **SHIPPED** durumuna geçirilir, veri repository üzerinden kaydedilir, aggregate'te biriken olaylar çekilir *(pullEvents)* ve gözlemcilere *(observer)* dağıtılır. Olay takibi yapan gözlemcilerden *(observers)* biri müşteriye e-posta göndermektedir. İlerleyen günlerde şöyle bir olay yaşanır; e-posta sunucusu çöker ve gözlemci **exception** fırlatır. Sonuç olarak sipariş, veritabanında **SHIPPED** durumunda kalır, müşteriye bildirim gitmez, kullanıcı hata mesajı görür ve işlemi tekrar denediğinde *"geçersiz durum geçişi"* hatası alır.

**Dual-write** problemi olarak da bilinen bu senaryoya yönelik en sağlam çözüm şıklardan hangisidir?

- A) Gözlemcideki hatayı **try-catch** ile yakalayıp loglamak. Kullanıcı hata görmeyeceği için sorun tamamen çözülür.
- B) Önce olayı yayınlamak, ardından siparişi kaydetmek. Böylece gözlemci hata verirse sipariş de kaydedilmez ve sorun tamamen ortadan kalkar.
- C) Olayı siparişle aynı veritabanı transaction'ı içinde bir **outbox** tablosuna yazmak ve ayrı bir sürecin bu tabloyu okuyup olayları yayınlamasını sağlamak. Olaylar en az bir kez *(at-least-once)* teslim edileceği için gözlemcilerin idempotent tasarlanmasını sağlamak.
- D) E-posta gönderme kodunu **Order.ship()** metodunun içine taşımak. Böylece kargolama ve bildirim tek bir metotta toplanır.

## Soru 15

Domain exception'larının istemcilere makine tarafından okunabilir bir hata kodu sunması için üst soyut sınıfa `code()` metodu eklenmiştir. Bu kod, sınıf adından otomatik olarak türetilmektedir.

```java
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public String code() {
        String name = getClass().getSimpleName().replaceFirst("Exception$", "");
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }
}
```

Örneğin **InvalidStateTransitionException** sınıfı için `code()` metodu `invalid-state-transition` değerini döner. İlerleyen günlerde ekibe yeni katılan bir geliştirici ödeme sağlayıcısındaki hataları ele almak için **PaymentAPIFailedException** isimli bir sınıf daha ekliyor.

```java
public class PaymentAPIFailedException extends DomainException {
    public PaymentAPIFailedException(String message) {
        super(message);
    }
}
```

Buna göre aşağıdaki kullanımın çıktısı ne olur?

```java
String code = new PaymentAPIFailedException("timeout").code();
System.out.println(code);
```

- A) `payment-api-failed`
- B) `PaymentAPIFailed`
- C) `payment-a-p-i-failed`
- D) `payment-apifailed`

## Soru 16

**PostgreSQL** üzerinde çalışan bir satış uygulamasında sipariş numaraları `nextval('orders_seq')` çağrısı ile sekanstan *(sequence)* alınmaktadır. Sipariş oluşturma senaryosu tek bir transaction içinde çalışmaktadır. Gün sonu raporunda **10250** numaralı siparişten sonra **10253** numaralı siparişin geldiği görülmüştür. Loglar incelendiğinde aradaki iki sipariş oluşturma isteğinin müşteri bulunamadığı için hata verdiği ve transaction'ların geri alındığı *(rollback)* tespit edilmiştir. Her iki istekte de `nextval` çağrısı müşteri kontrolünden önce yapılmıştır.

Sizce bu durum için aşağıdaki açıklamalardan hangisi doğrudur?

- A) Veritabanı sekansları performans nedeniyle transaction'lardan bağımsız çalışır. Rollback, `nextval` ile alınmış numarayı geri vermez. Bu yüzden boşluk *(gap)* oluşması beklenen bir durumdur. Boşluksuz numaralandırma yasal bir zorunluluksa *(örneğin fatura numaraları)* farklı bir çözüm tasarlanmalıdır.
- B) Rollback işlemi başarısız olmuştur ve iki yarım sipariş veritabanında kalmıştır. Bu kayıtlar bulunup elle silinmelidir.
- C) Sekanslar her rollback'te otomatik olarak geri alınır. Boşluk ancak birisi kayıtları elle sildiğinde oluşabilir.
- D) Bu durum sadece uygulama sunucusu yeniden başlatıldığında yaşanır. Sunucu yeniden başlatılmadığı sürece sekans her zaman ardışık değer üretir.

## Soru 17

**Jakarta EE** ortamında çalışan bir sipariş servisinde denetim kaydının *(audit log)* ana işlem geri alınsa bile kalıcı olması istenmektedir. Bu nedenle `audit` metodu yeni bir transaction açacak şekilde `@Transactional(TxType.REQUIRES_NEW)` anotasyonu ile işaretlenmiştir.

```java
@ApplicationScoped
public class OrderService {

    @Transactional(TxType.REQUIRED)
    public void ship(int orderId) {
        // siparişi oku, kargola, kaydet...
        this.audit(orderId, "ship requested");
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void audit(int orderId, String message) {
        // denetim kaydını veritabanına yaz...
    }
}
```

Bir istemci, CDI *(Contexts and Dependency Injection)* tarafından enjekte edilen **OrderService** örneği üzerinden `ship(10248)` çağrısı yaptığında **audit** metodu nasıl çalışır? *(Kullanılan CDI uygulaması aynı bean içindeki doğrudan metot çağrılarını intercept etmeMEktedir)*

- A) **audit** metodu `REQUIRES_NEW` ile işaretlendiği için yeni bir transaction içinde çalışır. **ship** geri alınsa bile denetim kaydı kalıcı olur.
- B) `@Transactional` davranışı CDI'ın ürettiği proxy üzerindeki interceptor ile sağlanır. `this.audit(...)` çağrısı proxy'den geçmediği için *(self-invocation)* `REQUIRES_NEW` ayarı devreye girmez ve **audit**, **ship** metodunun transaction'ı içinde çalışır. **ship** geri alınırsa denetim kaydı da geri alınır.
- C) Aynı sınıf içinde farklı **TxType** değerleri kullanılamayacağı için kod derlenmez.
- D) **audit** çağrıldığında aktif bir transaction bulunduğu için **TransactionRequiredException** fırlatılır.

## Soru 18

Bir sipariş API'sinde aynı taslak sipariş için **confirm** ve **cancel** istekleri aynı anda, paralel olarak gönderiliyor. Her iki istek de kendi transaction'ında siparişi veritabanından okuyor, domain metodunu çağırıyor ve son olarak ilgili siparişi kaydediyor. Testlerde bazı siparişler için iki isteğin de **200 OK** döndüğü ancak siparişin son durumunun **CONFIRMED** olduğu görülüyor. Yani başarılı yanıt verilmiş bir iptal işlemi kayboluyor *(lost update)*.

Sizce bu sorunu çözmek için en uygun yaklaşım şıklardan hangisidir?

- A) **Order** sınıfının **confirm** ve **cancel** metotlarını `synchronized` olarak işaretlemek.
- B) Hiçbir değişiklik yapmamak. İstekler zaten JTA transaction'ları içinde çalıştığı için veritabanı bu yarış durumunu *(race condition)* kendiliğinden engeller.
- C) Siparişler tablosuna bir **version** kolonu eklemek ve entity'de `@Version` ile işaretlemek *(optimistic locking)*. Güncelleme `UPDATE ... WHERE order_id = ? AND version = ?` şeklinde çalışır. Hiçbir satır etkilenmezse JPA, flush veya commit sırasında bir `OptimisticLockException` fırlatır. API bu çakışmayı HTTP **409 Conflict** yanıtına eşler. İstemci siparişi yeniden okuyup kararını tekrar verir.
- D) İstemcilerden aynı sipariş için istekleri her zaman sırayla göndermelerini istemek ve bunu API dokümantasyonuna yazmak.

## Soru 19

Üretimdeki ERP sisteminde bir yavaşlık yaşandığı bilgisi geliyor. İnceleme sırasında sipariş listesi ekranının yavaş açıldığı tespit ediliyor ve veritabanı loglarına bakılıyor. 100 siparişlik bir liste için önce siparişleri getiren bir sorgu, ardından her sipariş için o siparişe ait kalemleri getiren ayrı bir sorgu çalıştığı, yani toplamda **101** sorgu gönderildiği fark ediliyor. Liste büyüdükçe sorgu sayısı da doğrusal olarak artıyor.

Bu problem literatürde nasıl adlandırılır ve en uygun çözüm hangisidir?

- A) Bu bir **N+1** problemidir. Siparişler okunduktan sonra ilgili tüm kalemler tek bir sorgu ile okunup `order_id` değerine göre bir **Map** üzerinde gruplanabilir. Böylece sipariş sayısından bağımsız olarak sabit sayıda sorgu çalışır *(JPA kullanılıyorsa `JOIN FETCH` benzer bir çözüm sunar)*.
- B) Bu bir **deadlock** problemidir. Her kalem sorgusu için ayrı bir thread açılarak sorgular paralel çalıştırılmalıdır.
- C) Bu bir **SQL injection** problemidir. Sorgular `PreparedStatement` ve `?` parametreleri ile yazılırsa sorgu sayısı azalır.
- D) Bu bir **connection leak** problemidir. Her sorgu için yeni bir veritabanı bağlantısı açılması sorunu çözer.

## Soru 20

Bir satış modülü tek bir Maven modülü *(tek JAR)* olarak geliştirilmektedir ve içinde `sales.domain`, `sales.application` ve `sales.persistence` paketleri bulunmaktadır. Ekip bir mimari karar kaydında *(ADR - Architecture Decision Record)* domain paketinin hiçbir koşulda persistence paketine bağımlı olmayacağını belirlemiştir. Ancak bir geliştirici **Order** sınıfını aşağıdaki gibi değiştirdiğinde derleme sorunsuz geçmekte ve tüm birim testler yeşil bayrak kaldırmaktadır.

```java
package sales.domain;

import sales.persistence.OrderRepository;

public final class Order {
    private OrderRepository orderRepository;
    // Diğer alanlar ve metotlar...
}
```

Sizce bu durumun sebebi ve kuralı korumanın en uygun yolu hangisidir?

- A) Maven'da persistence bağımlılığını `provided` scope ile tanımlamak bu bağımlılığı derleme zamanında engeller.
- B) Persistence paketindeki tüm sınıfları `package-private` yapmak yeterlidir. Böylece hiçbir paket bu sınıflara erişemez ve kural kendiliğinden korunur.
- C) `Domain` ve `persistence` sınıflarını aynı paket altında birleştirmek gerekir. Böylece paketler arası bağımlılık sorunu ortadan kalkar.
- D) Aynı **JAR** içindeki paketler arasındaki bağımlılığı **Maven** veya derleyici tek başına engelleyemez. Kural, **ArchUnit** gibi bir araçla yazılan bir mimari testle *(örneğin "domain paketindeki sınıflar persistence paketine bağımlı olamaz")* korunmalı ve bu test ilgili ADR ile ilişkilendirilmelidir.

---

## Cevap Anahtarı

| Soru No | Doğru Cevap |
| ------- | ----------- |
| 1 | B |
| 2 | D |
| 3 | C |
| 4 | A |
| 5 | B |
| 6 | C |
| 7 | A |
| 8 | D |
| 9 | B |
| 10 | C |
| 11 | A |
| 12 | D |
| 13 | B |
| 14 | C |
| 15 | D |
| 16 | A |
| 17 | B |
| 18 | C |
| 19 | A |
| 20 | D |
