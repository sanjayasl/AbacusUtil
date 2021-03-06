### 1.2.2

* Rename `JdbcUtil.absolute` to `JdbcUtil.skip`.

* Add `JdbcUtil.getColumnLabelList`.

* Add `Nullable.mapToBoolean/Char/Byte/.../Double`.

* Add `Nullable.mapToBoolean/Char/Byte/.../DoubleIfNotNull`.

* Add `CassandraExecutor/MongoDBExecutor/CouchbaseExecutor.queryForBoolean/Char/Byte/.../Double/String`.

* Improvements and bug fix.


### 1.2.1

* Remove `Mapper/AsyncMapper.stream(Connection...)`.

* Remove `Try.reader/writer/stream/callable`.

* Remove `N.findAll(..., Function...)`.

* Rename `N.findAllIndices` to `findAllIndicesBetween`, `N.findAll` to `findAllSubstringsBetween`.

* Rename `Splitter.split(..., Function...)` to `Splitter.splitAndThen(..., Function...)`

* Add `N.wrap/unwrap`.

* Add `N.flatMap/flattMap`.

* Add `N.EMPTY_BOOLEAN/CHAR/BYTE/SHORT/INT/../DOUBLE_OBJ_ARRAY`.

* Add `N.sleep/run/callUninterruptibly`, copied from Google Guava.

* Add `Stopwatch/RateLimiter`, copied from Google Guava.

* Improvements and bug fix.


### 1.2.0

* Rename `flatCollection/flatArray` to `flattMap/flatMapp`.

* Add `Array.asList`.

* Add `N.newArrayDeque/isMixedCase/appendIfMissing/prependIfMissing/wrapIfMissing`.

* Rename `N.between` to `N.substringBetween`.

* Add `SafeInitializer`, copied from Apache Commons Lang.

* Improvements and bug fix.


### 1.1.9

* Rename `Mapper.WriteOnly/registerWriteOnlyProps` to `Mapper.NonUpdatable/registerNonUpdatableProps`.

* Rename `iteratee/mergee/parallelMergee/summarizee/summingDoublee/averagingDoublee/reshapee/invokee/onTextChangedd/beforeTextChangedd/afterTextChangedd` to `iteratte/mergge/parallelMergge/summarizze/summingDoubble/averagingDoubble/reshappe/invokke/onTextChangged/beforeTextChangged/afterTextChangged`.

* Add `Mapper.findAll/queryAll/streamAll`.

* Improvements and bug fix.


### 1.1.8

* Add `JdbcUtil.stream/executeBatchUpdate/absolute(...)`.

* Improvements and bug fix.


### 1.1.7

* Add `DataSetUtil`.

* Add `DataSet.rollup/cube`.

* Add `Optional/Nullable<T>.or(Try.Supplier<? extends Optional<T>, E> supplier)`.

* Rename `Math2` to `Maths`.

* Rename `xyz0/1/2(...)` to `xyzz(...)`.

* Rename `DataSet.retainAll/removeAll` to `DataSet.intersectAll/except`.

* Remove `DataSet.sum/averageInt/Long/Double/kthLargest/count/toMultiset/toArray/split`, replaced by `DataSet.stream(...).sum/averageInt/Long/Double/kthLargest/count/toMultiset/toArray/split`.

* Improvements and bug fix.


### 1.1.6

* Add `DataSet.groupBy(..., func)/getOrDefault(columnName)`.

* Add `Collectors.combine(...)`.

* Add `Comparators.comparingByLength/comparingBySize`.

* Rename `Seq/Iterators.repeat(Collection)` to `Seq/Iterators.repeatt(Collection)`.

* Improvements and bug fix.


### 1.1.5

* Remove `Maps.diff`.

* Remove `f.matrix(...)`.

* Remove `Stream.unzip/unzip3`, replaced by `Seq/Iterators.unzip/unzip3`.

* Rename `Fn.indexeD` to `Fn.indexedd`, `Fn.indeXed` to `Fn.indexeed`.

* Rename `SQLExecutor.geT` to `SQLExecutor.gett`.

* Add `Optional<T> MongoDBExecutor/CassandraExecutor/CouchbaseExecutor/SQLiteExecutor.gett(...)`.

* Add `Multiset/LongMultiset/Multimap/ListMultimap/SetMulitmap/BiMap.copy()`.

* Add `Multimap/ListMultimap/SetMultimap.concat(...)`.

* Add `Multimap.toMultiset()`.

* Add `Multimap.totalCountOfValue()`.

* Add `ListMultimap/SetMulitmap.toImmutableMap`.

* Add `N.forEach(int startInclusive, int endExclusive...)`.

* Add `Matrix/IntMatrix.extend(...)`.


### 1.1.4

* Add `Multiset/LongMultiset.toImmutableMap`.

* Add `ImmutableSortedSet/ImmutableNavigableSet/ImmutableSortedMap/ImmutableNavigableMap/ImmutableBiMap`.

* Remove `Multimap.set/setAll/setIf/setAllIf`.

* Rename `Multimap.putAllIfAbsent` to `Multimap.putAllIfKeyAbsent`.

* Add `Multimap.putIfKeyAbsent`.

* Add `OptionalBoolean/OptionalByte/.../OptionalDouble.ofNullable`.

* Add `Optional<T> SQLExecutor.geT(...)`.

* Improvements and bug fix.


### 1.1.3

* Add `N.sumInt/sumLong/sumDouble/averageInt/averageLong/averageDouble` for `Number` type.

* Add `Fn.numToInt/numToLong/numToDouble`.

* Add `Multimap/ListMultimap/SetMultimap.invertFrom/flatInvertFrom`

* Improve `Futures`.

* Improvements and bug fix.


### 1.1.2

* Replace `Predicate/Consumer/Function` in `Iterators` with `Try.Predicate/Consumer/Function`.

* Replace `Predicate/Consumer/Function` in `Stream/.../IntStream.anyMatch/.../findFirst/findLast/...` with `Try.Predicate/Consumer/Function`.

* Rename `Maps.inverse` to `Maps.flatInvert`

* Rename `Fn.Consumers/BiConsumers/TriConsumers/Functions/BiFunctions/TriFunctions/Predicates.of(...)` to `create(...)`

* Remove `Fn.BiFunctions/TriFunctions.ofTuple()`, replaced with `Fn.tuple2()/tuple3()`.

* Remove `MultimapBuilder.removeAll(Collection<? extends K> keysToRemove)`.

* Add `MultimapBuilder.removeAll(K key, Collection<?> valuesToRemove)`.

* Add `Stream.partitionBy/partitionByToEntry/partitionTo`.

* Add `EntryStream/Stream.sortedByInt/sortedByLong/sortedByDouble`.

* Add `EntryStream.flatCollectionKey/flatCollectionValue`.

* Add `N.ifOrEmpty`.

* Add `If/IF`.

* Improvements and bug fix.


### 1.1.1

* Replace `Predicate/Consumer/Function` in `Multiset/LongMultiset/Multimap/Matrix/IntMatrix/.../f` with `Try.Predicate/Consumer/Function`.

* Remove `IOUtil.parseInt(...)/parseLong(...)`.

* Remove `N.copy(Object entity, boolean ignoreUnknownProperty, Set<String> ignorePropNames)`.

* Remove `N.asConcurrentMap/asBiMap(...)`, Replace with `BiMap.of(...)`.

* Remove `Stream.biMap/triMap(...)`, replaced with `Stream.slidingMap(mapper, 2, ignoreNotPaired)/slidingMap(mapper, 3, ignoreNotPaired)`

* Refactoring `N.merge`: change `merge(sourceEntity, selectPropNames, targetEntity)` to `merge(sourceEntity, targetEntity, selectPropNames)`.

* Add `Fn.tuple1/tuple2/tuple3/tuple4(...)`.

* Add `BooleanPair/BytePair/ShortPair/BooleanTriple/ByteTriple/ShortTriple`.

* Add `Maps.removeIf/removeIfKey/removeIfValue(...)`.

* Add `Maps.map2Entity(targetClass, map, selectPropNames)`.

* Add `N.newTreeMap(...)`.

* Add `Joiner.concat(...)`.

* Add `Median`.

* Improvements and bug fix.


### 1.1.0

* `Clazz.of(Class<?>)` is marked to ‘Deprecated’ and will be removed in version 1.2.0 because it doesn’t work as expected.

* Replace `Predicate/Consumer/Function` in `N/Seq/IntList.../EntryStream/Stream/IntStream/.../DataSet.forEach(...)` and `N/JdbcUtil/IOUtil.parse(...)` with `Try.Predicate/Consumer/Function`.

* Add `EntryStream.collect(java.util.stream.Collector)` and `EntryStream.collectAndThen(java.util.stream.Collector, Function)`.

* Add `Math2.asinh(double)/acosh(double)/atanh(double)`. Copied from Apache Commons Math.

* Add `N.deleteRange(boolean[] a, int fromIndex, int toIndex)/deleteRange(char[]...)/deleteRange(byte[]...)/.../deleteRange(List<T>...)`.

* Remove `IndexedIntConsumer...` and `BooleanList/CharList/ByteList/.../IntList.forEach(IndexedIntConsumer...)`.

* Improvements and bug fix.



### Prior 1.1.0
* Refer to: [CHANGES.txt](https://github.com/landawn/AbacusUtil/blob/master/CHANGES.txt)
