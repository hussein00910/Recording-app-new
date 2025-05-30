# تصميم واجهة المستخدم لتطبيق تسجيل المكالمات

## نظرة عامة على التصميم

سيتم تصميم تطبيق تسجيل المكالمات بواجهة مستخدم بسيطة وأنيقة، مع التركيز على سهولة الاستخدام والوصول السريع للوظائف الأساسية. سيتبع التصميم إرشادات Material Design مع تخصيصات لتناسب واجهة EMUI/HarmonyOS الخاصة بهواتف هواوي.

## الألوان الرئيسية

- **اللون الأساسي**: #2196F3 (أزرق)
- **اللون الثانوي**: #FFC107 (أصفر ذهبي)
- **لون الخلفية**: #FFFFFF (أبيض)
- **لون النص الأساسي**: #212121 (رمادي داكن)
- **لون النص الثانوي**: #757575 (رمادي متوسط)

## هيكل التطبيق

### 1. الشاشة الرئيسية (قائمة التسجيلات)

تعرض قائمة بجميع تسجيلات المكالمات مرتبة من الأحدث إلى الأقدم، مع المعلومات التالية لكل تسجيل:
- اسم جهة الاتصال أو رقم الهاتف
- نوع المكالمة (واردة/صادرة)
- تاريخ ووقت المكالمة
- مدة التسجيل
- مؤشر مرئي للتسجيلات المميزة بنجمة

**عناصر إضافية في الشاشة الرئيسية:**
- شريط بحث في الأعلى
- زر عائم (FAB) للوصول السريع إلى الإعدادات
- قائمة منسدلة للتصفية (جميع التسجيلات، المميزة بنجمة، الواردة، الصادرة)
- مؤشر لحالة التسجيل التلقائي (مفعل/معطل)

### 2. شاشة تشغيل التسجيل

تظهر عند النقر على أي تسجيل في القائمة، وتتضمن:
- معلومات التسجيل (الاسم، التاريخ، المدة)
- شريط تقدم التشغيل مع إمكانية التحكم
- أزرار التحكم (تشغيل/إيقاف مؤقت، تقديم، إرجاع)
- خيارات إضافية (مشاركة، حذف، تمييز بنجمة، إضافة ملاحظة)
- رسم بياني للصوت أثناء التشغيل

### 3. شاشة الإعدادات

تتضمن جميع خيارات تكوين التطبيق:
- تفعيل/تعطيل التسجيل التلقائي لجميع المكالمات
- تفعيل/تعطيل التسجيل التلقائي لجهات اتصال محددة
- اختيار مجلد حفظ التسجيلات
- تحديد جودة التسجيل (عالية، متوسطة، منخفضة)
- خيارات تنبيهات التسجيل (صوت، اهتزاز، إشعار)
- إعدادات الخصوصية (حماية بكلمة مرور، إخفاء التطبيق)
- خيارات تحسين الصوت

### 4. شاشة جهات الاتصال

تعرض قائمة جهات الاتصال مع خيار تحديد من سيتم تسجيل مكالماتهم تلقائياً:
- قائمة بجميع جهات الاتصال
- خيار البحث
- مفتاح تبديل بجانب كل جهة اتصال لتفعيل/تعطيل التسجيل التلقائي

### 5. شاشة الترحيب والإعداد الأولي

تظهر عند تشغيل التطبيق لأول مرة:
- شرح موجز لوظائف التطبيق
- طلب الأذونات اللازمة (الميكروفون، سجل المكالمات، التخزين)
- إعداد الخيارات الأساسية
- شرح كيفية التعامل مع قيود النظام (إن وجدت)

## تدفق المستخدم

1. **التشغيل الأول**: شاشة الترحيب → طلب الأذونات → الإعداد الأولي → الشاشة الرئيسية
2. **التشغيل العادي**: الشاشة الرئيسية مباشرة
3. **أثناء مكالمة**: إشعار بحالة التسجيل → خيار بدء/إيقاف التسجيل
4. **بعد التسجيل**: إشعار باكتمال التسجيل → إضافة التسجيل للقائمة الرئيسية

## عناصر التصميم المميزة

1. **وضع الليل/النهار**: تبديل تلقائي بين السمة الفاتحة والداكنة حسب إعدادات النظام
2. **رسوم متحركة سلسة**: انتقالات وتأثيرات حركية بسيطة لتحسين تجربة المستخدم
3. **أيقونات واضحة**: استخدام أيقونات Material Design مع تسميات توضيحية
4. **تصميم متجاوب**: يعمل بشكل مثالي على مختلف أحجام الشاشات وتوجيهاتها
5. **شريط تنقل سفلي**: للوصول السريع إلى الأقسام الرئيسية (التسجيلات، جهات الاتصال، الإعدادات)

## اعتبارات خاصة لهواتف هواوي

1. تخصيص بعض عناصر الواجهة لتتناسب مع أسلوب EMUI/HarmonyOS
2. دعم الإيماءات الكاملة لنظام التشغيل
3. تحسين استهلاك البطارية بما يتوافق مع آليات توفير الطاقة في هواوي
4. التوافق مع خدمات هواوي بدلاً من خدمات جوجل عند الضرورة

## الشاشات الرئيسية (مخطط تفصيلي)

```
+----------------------------------+
|  تسجيل المكالمات     🔍  ⋮      |
+----------------------------------+
|  تصفية: جميع التسجيلات    ▼    |
+----------------------------------+
|  محمد أحمد                      |
|  ↘️ مكالمة واردة                |
|  21 مايو، 10:30 ص   3:45        |
+----------------------------------+
|  سارة محمود            ⭐       |
|  ↗️ مكالمة صادرة                |
|  20 مايو، 2:15 م     1:30       |
+----------------------------------+
|  01234567890                     |
|  ↘️ مكالمة واردة                |
|  19 مايو، 9:45 م     0:45       |
+----------------------------------+
|                                  |
|                                  |
|                                  |
|                                  |
|                                  |
|                  [➕]            |
+----------------------------------+
| التسجيلات | جهات الاتصال | الإعدادات |
+----------------------------------+
```

## الخطوات التالية

بعد الموافقة على هذا التصميم، سيتم:
1. إنشاء نماذج أولية للواجهة (Mockups)
2. تحديد هيكل الكود والمكتبات اللازمة
3. البدء في تطوير التطبيق الفعلي
