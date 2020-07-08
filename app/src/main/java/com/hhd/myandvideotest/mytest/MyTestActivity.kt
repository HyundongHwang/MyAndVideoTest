package com.hhd.myandvideotest.mytest

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import com.hhd.myandvideotest.util.LogEx
import com.hhd.myandvideotest.util.MyActivityUtil
import com.hhd.myandvideotest.util.MyUtil
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.AsyncSubject
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MyTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MyActivityUtil.setActionBarHide(this)
        MyActivityUtil.setKeepScreenOn(this)
        val context = this as Context
        val thisObj = this
        val sv = MyUtil.createScrollViewMpWc(context)
        val fl = MyUtil.createFlexboxLayoutMpWc(context)
        this.setContentView(sv)
        sv.addView(fl)

        this.javaClass.methods
            .filter { it.name.contains("_") }
            .filterNot { it.name.contains("$") }
            .forEach {
                val method = it
                val btn = Button(context)
                fl.addView(btn)
                btn.isAllCaps = false
                btn.text = method.name
                btn.setOnClickListener { method.invoke(thisObj) }
            }
    }


    fun _01_Observable_subscribeBy() {
        val ob = listOf("one", "two", "three").toObservable()
        ob.subscribeBy(
            onNext = { LogEx.d("onNext it[$it]") },
            onError = { LogEx.d("onError it[$it]") },
            onComplete = { LogEx.d("onComplete") }
        )
    }

    fun _02_PublishSubject_map_subscribe() {
        val sb = PublishSubject.create<Int>()

        sb.map {
            LogEx.d("map it[$it]")
            it % 2 == 0
        }.subscribe({
            LogEx.d("subscribe it[$it]")
        })

        sb.onNext(4)
        sb.onNext(9)
    }

    fun _03_Maybe_just() {
        val mb = Maybe.just(14)
        mb.subscribeBy(
            onSuccess = {
                LogEx.d("onSuccess it[$it]")
            },
            onError = {
                LogEx.d("onError it[$it]")
            },
            onComplete = {
                LogEx.d("onComplete")
            }
        )
    }

    class _MyBasicObserver : Observer<Any> {
        override fun onComplete() {
            LogEx.d("${this.hashCode()} onComplete")
            LogEx.d("")
        }

        override fun onSubscribe(d: Disposable) {
            LogEx.d("${this.hashCode()} onSubscribe d[$d]")
        }

        override fun onNext(t: Any) {
            LogEx.d("${this.hashCode()} onNext t[$t]")
        }

        override fun onError(e: Throwable) {
            LogEx.d("${this.hashCode()} onError e[$e]")
        }
    }

    fun _04_Observable_subscribe() {
        listOf(1, 2, 3, 4)
            .toObservable()
            .subscribe(_MyBasicObserver())
    }

    fun _05_Observable_just() {
        Observable.just(1).subscribe(_MyBasicObserver())
        Observable.just(10, 20, 30).subscribe(_MyBasicObserver())
        Observable.just(listOf(100, 200, 300)).subscribe(_MyBasicObserver())
    }

    fun _06_Observable_interval() {
        Observable.interval(100, TimeUnit.MILLISECONDS).takeWhile { it < 100 }
            .subscribe(_MyBasicObserver())
    }

    fun _07_hot_Observable_ConnectableObservable_publish_connect() {
        val cob = listOf(1, 2, 3).toObservable().publish()
        cob.subscribe(_MyBasicObserver())
        cob.connect()
        cob.subscribe(_MyBasicObserver())
    }

    fun _08_PublishSubject_mid_subscribe() {
        val ob = Observable.interval(100, TimeUnit.MILLISECONDS).takeWhile { it < 50 }
        val ps = PublishSubject.create<Long>()
        ob.subscribe(ps)
        ps.subscribe(_MyBasicObserver())

        runBlocking {
            delay(2_000)
        }

        ps.subscribe(_MyBasicObserver())

        runBlocking {
            delay(2_000)
        }
    }

    fun _09_AsyncSubject_basic() {
        val ob = Observable.just(1, 2, 3)
        val aSub = AsyncSubject.create<Int>()
        ob.subscribe(aSub)
        aSub.subscribe(_MyBasicObserver())
        aSub.onComplete()
    }

    fun _10_AsyncSubject_2_Observer() {
        val aSub = AsyncSubject.create<Int>()
        aSub.onNext(1)
        aSub.onNext(2)
        aSub.onNext(3)
        aSub.subscribe(_MyBasicObserver())

        aSub.onNext(4)
        aSub.onNext(5)
        aSub.onNext(6)
        aSub.subscribe(_MyBasicObserver())

        aSub.onComplete()
    }

    fun _11_PublishSubject_basic() {
        val pSub = PublishSubject.create<Int>()
        pSub.subscribe(_MyBasicObserver())
        pSub.onNext(1)
        pSub.onNext(2)
        pSub.onNext(3)

        pSub.subscribe(_MyBasicObserver())
        pSub.onNext(4)
        pSub.onNext(5)
        pSub.onNext(6)

        pSub.onComplete()
    }

    fun _12_BehaviorSubject_very_last() {
        val bSub = BehaviorSubject.create<Int>()
        bSub.onNext(1)
        bSub.onNext(2)
        bSub.onNext(3)
        bSub.subscribe(_MyBasicObserver())

        bSub.onNext(4)
        bSub.onNext(5)
        bSub.onNext(6)
        bSub.subscribe(_MyBasicObserver())

        bSub.onNext(7)
        bSub.onNext(8)
        bSub.onNext(9)
        bSub.onComplete()
    }

    fun _13_ReplaySubject_all() {
        val rSub = ReplaySubject.create<Int>()
        rSub.onNext(1)
        rSub.onNext(2)
        rSub.onNext(3)
        rSub.subscribe(_MyBasicObserver())

        rSub.onNext(4)
        rSub.onNext(5)
        rSub.onNext(6)
        rSub.subscribe(_MyBasicObserver())

        rSub.onNext(7)
        rSub.onNext(8)
        rSub.onNext(9)
        rSub.onComplete()
    }

    fun _14_BehaviorSubject_observeOn_computation_bug() {
        val ob = Observable.just(1, 2, 3)
        val bSub = BehaviorSubject.create<Int>()

        bSub.observeOn(Schedulers.computation())
            .subscribe {
                LogEx.d("${hashCode()} subscribe it[$it] start")
                runBlocking { delay(1_000) }
                LogEx.d("${hashCode()} subscribe it[$it] end")
            }

        bSub.observeOn(Schedulers.computation())
            .subscribe {
                LogEx.d("${hashCode()} subscribe it[$it] received!!!")
            }

        ob.subscribe(bSub)
    }

    fun _15_BehaviorSubject_observeOn_computation_bug_2() {
        Observable.range(0, 10)
            .map {
                LogEx.d("map ${it}")
                it
            }
            .observeOn(Schedulers.computation())
            .subscribe {
                LogEx.d("subscribe $it start")
                runBlocking { delay(1_000) }
                LogEx.d("subscribe $it end")
            }
    }

    fun _16_Flowable_basic() {
        Flowable.range(0, 10)
            .map {
                LogEx.d("map ${it}")
                it.toString()
            }
            .observeOn(Schedulers.io())
            .subscribe {
                LogEx.d("subscribe $it start")
                runBlocking { delay(1_000) }
                LogEx.d("subscribe $it end")
            }
    }

    fun _17_Flowable_Subscription_request() {
        Flowable.range(1, 10)
            .observeOn(Schedulers.io())
            .subscribe(object : Subscriber<Int> {
                override fun onComplete() {
                    LogEx.d("onComplete")
                }

                override fun onSubscribe(s: Subscription?) {
                    s!!.request(5)
                }

                override fun onNext(t: Int?) {
                    LogEx.d("onNext [$t]")
                }

                override fun onError(t: Throwable?) {
                }
            })
    }

    private fun _run_Flowable(st: BackpressureStrategy) {
        Observable
            .range(1, 1_000)
            .toFlowable(st)
            .map {
                LogEx.d("map $it")
                it.toString()
            }
            .observeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    LogEx.d("onNext $it start")
                    runBlocking { delay(10) }
                    LogEx.d("onNext $it end")
                },
                onError = {
                    LogEx.e("onError $it")
                })
    }

    fun _18_BackpressureStrategy_BUFFER() {
        _run_Flowable(BackpressureStrategy.BUFFER)
    }

    fun _19_BackpressureStrategy_ERROR() {
        _run_Flowable(BackpressureStrategy.ERROR)
    }

    fun _20_BackpressureStrategy_LATEST() {
        _run_Flowable(BackpressureStrategy.LATEST)
    }

    fun _21_BackpressureStrategy_DROP() {
        _run_Flowable(BackpressureStrategy.DROP)
    }

    @SuppressLint("CheckResult")
    fun _22_BackpressureStrategy_MISSING_buf_10() {
        (1..1_000).toList().toFlowable()
            .onBackpressureBuffer(
                10,
                { LogEx.d("overflow") },
                BackpressureOverflowStrategy.DROP_OLDEST
            )
//                .onBackpressureDrop()
//                .onBackpressureLatest()
            .observeOn(Schedulers.computation(), false, 1)
            .map {
                LogEx.d("map $it")
                it.toString()
            }
            .observeOn(Schedulers.computation())
            .subscribeBy(
                onNext = {
                    LogEx.d("onNext $it start")
                    runBlocking { delay(1000) }
                    LogEx.d("onNext $it end")
                },
                onError = {
                    LogEx.e("onError $it")
                })
    }

    fun _23_ConnectableFlowable_publish_connect() {
        val conFw = listOf(1, 2, 3).toFlowable().publish()

        conFw.subscribe {
            LogEx.d("1st subscribe $it start")
            runBlocking { delay(1_000) }
            LogEx.d("1st subscribe $it end")
        }

        conFw.subscribe {
            LogEx.d("2nd subscribe $it start")
            LogEx.d("2nd subscribe $it end")
        }

        conFw.subscribe {
            LogEx.d("3rd subscribe $it start")
            LogEx.d("3rd subscribe $it end")
        }

        conFw.connect()
    }

    fun _24_Flowable_PublishProcessor_subscribe() {
        val fw = listOf(1, 2, 3).toFlowable()
        val pp = PublishProcessor.create<Int>()

        pp.subscribe {
            LogEx.d("1st subscribe $it start")
            runBlocking { delay(1_000) }
            LogEx.d("1st subscribe $it end")
        }

        pp.subscribe {
            LogEx.d("2nd subscribe $it start")
            LogEx.d("2nd subscribe $it end")
        }

        pp.subscribe {
            LogEx.d("3rd subscribe $it start")
            LogEx.d("3rd subscribe $it end")
        }

        fw.subscribe(pp)
    }

    fun _25_Flowable_buffer_bound() {
        val boundFw = Flowable.interval(350, TimeUnit.MILLISECONDS)
        val srcFw = Flowable.interval(100, TimeUnit.MILLISECONDS)
        srcFw.buffer(boundFw).subscribe {
            LogEx.d("subscribe it[$it]")
        }
    }

    fun _26_Flowable_throttleFirst() {
        val fw = Flowable.interval(100, TimeUnit.MILLISECONDS)

        fw.throttleFirst(1_000, TimeUnit.MILLISECONDS)
            .subscribe {
                LogEx.d("subscribe it[$it]")
            }
    }

    fun _27_debounce() {
        Observable
            .create<Int> {
                it.onNext(1)
                runBlocking { delay(1_000) }
                it.onNext(2)
                runBlocking { delay(1_000) }
                it.onNext(3)
                runBlocking { delay(1_000) }
                it.onComplete()
            }
            .debounce(5_000, TimeUnit.MILLISECONDS)
            .subscribe {
                LogEx.d("subscribe $it")
            }
    }

    fun _28_Flowable_filter() {
        (1..100).toList().toFlowable().filter { it % 10 == 0 }.subscribe {
            LogEx.d("subscribe $it")
        }
    }

    fun _29_Observable_flatMap() {
        (0..10).toList().toObservable().flatMap { num ->

            val ob = arrayOf(
                num * 10,
                num * 10 + 1,
                num * 10 + 2
            ).toObservable()

            return@flatMap ob
        }.subscribe {
            LogEx.d("subscribe $it")
        }
    }

    fun _30_Observable_reduce() {
        (0..10).toList().toObservable().reduce { accum, newValue ->
            val res = accum + newValue
            res
        }.subscribe {
            LogEx.d("subscribe $it")
        }
    }

    fun _31_Observable_zip() {
        val ob1 = Observable.interval(100, TimeUnit.MILLISECONDS)
        val ob2 = Observable.interval(250, TimeUnit.MILLISECONDS)

        Observable.zip(ob1, ob2, BiFunction { t1: Long, t2: Long ->
            "$t1 $t2"
        }).subscribe {
            LogEx.d("subscribe $it")
        }
    }

    fun _32_Observable_combineLatest() {
        val ob1 = Observable.interval(100, TimeUnit.MILLISECONDS)
        val ob2 = Observable.interval(250, TimeUnit.MILLISECONDS)

        Observable.combineLatest(ob1, ob2, BiFunction { t1: Long, t2: Long ->
            "$t1 $t2"
        }).subscribe {
            LogEx.d("subscribe $it")
        }
    }

    fun _33_Observable_merge() {
        val ob1 = (0..10).toList().toObservable()
        val ob2 = (100..110).toList().toObservable()

        Observable.merge(ob1, ob2).subscribe {
            LogEx.d("subscribe $it")
        }
    }

    fun _34_Observable_amb() {
        val ob1 = Observable.interval(100, TimeUnit.MILLISECONDS).map { "ob1 $it" }
        val ob2 = Observable.interval(500, TimeUnit.MILLISECONDS).map { "ob2 $it" }

        Observable.amb(listOf(ob1, ob2))
            .subscribe {
                LogEx.d("subscribe $it")
            }
    }

    fun _35_video_frame_emul() {

        Flowable
            .create<Int>({
                val em = it
                (0..10).forEach {
                    runBlocking { delay(100) }
                    LogEx.d("${Thread.currentThread().name} src $it")
                    em.onNext(it)
                }
                em.onComplete()
            }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.newThread())
//                .observeOn(Schedulers.newThread(), false, 1)
            .observeOn(Schedulers.io())
            .map {
                runBlocking { delay(500) }
                LogEx.d("${Thread.currentThread().name} map $it")
                val newStr = "$it $it"
                newStr
            }
//                .observeOn(Schedulers.newThread(), false, 1)
            .observeOn(Schedulers.io())
            .map {
                runBlocking { delay(700) }
                LogEx.d("${Thread.currentThread().name} map2 $it")
                val newStr = "$it $it"
                newStr
            }
//                .observeOn(Schedulers.newThread(), false, 1)
            .observeOn(Schedulers.io())
            .subscribe {
                LogEx.d("${Thread.currentThread().name} subscribe1 $it")
            }

    }

    fun _36_Observable_flatMap_newThread() {

        (1..10)
            .toList()
            .toObservable()
            .observeOn(Schedulers.newThread())
            .flatMap {
                runBlocking { delay(Random().nextInt(3).toLong() * 1_000) }
                LogEx.d("flatMap $it")
                val num = it

                Observable.create<Int> {
                    val em = it
                    (0..10).forEach {
                        val newNum = num * 10 + it
                        LogEx.d("flatMap onNext $newNum")
                        em.onNext(newNum)
                    }
                    em.onComplete()
                }
            }
            .observeOn(Schedulers.newThread())
            .subscribe {
                LogEx.d("subscribe $it")
            }
    }


    fun _37_sequenceEqual() {
        val ob1 = arrayOf(1, 2, 3).toObservable()
        val ob2 = arrayOf(1, 2, 3).toObservable()

        Observable
            .sequenceEqual(ob1, ob2)
            .subscribe { b, t ->
                LogEx.d("onCallback b[$b]")
            }
    }

    fun _38_Schedulers_FixedThreadPool() {

        val sch = Schedulers.from(Executors.newFixedThreadPool(1))

        Single.just(Any())
            .map {
                LogEx.d("[${Thread.currentThread().name}] map 10")
                runBlocking { delay(4_000) }
                LogEx.d("[${Thread.currentThread().name}] map 11")
            }
            .observeOn(sch)
            .map {
                LogEx.d("[${Thread.currentThread().name}] map 20")
                runBlocking { delay(3_000) }
                LogEx.d("[${Thread.currentThread().name}] map 21")
            }
            .observeOn(sch)
            .map {
                LogEx.d("[${Thread.currentThread().name}] map 30")
                runBlocking { delay(2_000) }
                LogEx.d("[${Thread.currentThread().name}] map 31")
            }
            .observeOn(sch)
            .subscribe { _ ->
                LogEx.d("[${Thread.currentThread().name}] subscribe 40")
                runBlocking { delay(1_000) }
                LogEx.d("[${Thread.currentThread().name}] subscribe 41")
            }
    }


    private val _obj = Object()

    fun _39_Object_wait() {

        Single.just(Any())
            .observeOn(Schedulers.io())
            .subscribe { _ ->
                synchronized(_obj) {
                    LogEx.d("[${Thread.currentThread().name}] wait subscribe 10")
                    _obj.wait()
                    LogEx.d("[${Thread.currentThread().name}] wait subscribe 11")
                }
            }
    }

    fun _40_Object_notifyAll() {

        Single.just(Any())
            .observeOn(Schedulers.io())
            .subscribe { _ ->
                synchronized(_obj) {
                    LogEx.d("[${Thread.currentThread().name}] notifyAll subscribe 10")
                    _obj.notifyAll()
                    LogEx.d("[${Thread.currentThread().name}] notifyAll subscribe 11")
                }
            }
    }

    private val _reLock = ReentrantLock()
    private val _reLockCond = _reLock.newCondition()


    fun _41_ReentrantLock_await() {

        Single.just(Any())
            .observeOn(Schedulers.io())
            .subscribe { _ ->
                LogEx.d("[${Thread.currentThread().name}] subscribe 10")
                _reLock.withLock {
                    _reLockCond.await()
                }
                LogEx.d("[${Thread.currentThread().name}] subscribe 11")
            }
    }

    fun _42_ReentrantLock_signalAll() {
        _reLock.withLock {
            _reLockCond.signalAll()
        }
    }

    fun _43_Single_just_delay() {
        LogEx.d("[${Thread.currentThread().name}] start")

        Single.just(Any())
            .observeOn(Schedulers.io())
            .map { LogEx.d("[${Thread.currentThread().name}] map0") }
            .delay(10, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map { LogEx.d("[${Thread.currentThread().name}] map1") }
            .subscribe()
    }

    private val _pubSub_signal_emul = PublishSubject.create<Array<Any>>()

    fun _44_signal_emul() {
        _pubSub_signal_emul
            .observeOn(Schedulers.io())
            .map {
                val cmd = it[0]

                when (cmd) {
                    "add" -> {
                        val res = (it[1] as Int) + (it[2] as Int)
//                            runBlocking { delay(1_000) }
                        LogEx.d("[${Thread.currentThread().name}] add res[$res]")
                    }
                }
                return@map it
            }
            .observeOn(Schedulers.io())
            .map {
                val cmd = it[0]

                when (cmd) {
                    "sub" -> {
                        val res = (it[1] as Int) - (it[2] as Int)
                        runBlocking { delay(3_000) }
                        LogEx.d("[${Thread.currentThread().name}] sub res[$res]")
                    }
                }
                return@map it
            }
            .subscribe()
    }

    fun _45_signal_add() {
        _pubSub_signal_emul.onNext(arrayOf("add", 1, 2))
        _pubSub_signal_emul.onNext(arrayOf("add", 3, 4))
        _pubSub_signal_emul.onNext(arrayOf("add", 5, 6))
    }

    fun _46_signal_sub() {
        _pubSub_signal_emul.onNext(arrayOf("sub", 2, 1))
        _pubSub_signal_emul.onNext(arrayOf("sub", 3, 1))
        _pubSub_signal_emul.onNext(arrayOf("sub", 4, 1))
    }

    fun _47_ByteBuffer_copy() {
        val buf = ByteBuffer.wrap("helloworld".toByteArray())
        _log_ByteBuffer(buf, "buf")
        buf.position(2)
        buf.limit(5)
        _log_ByteBuffer(buf, "buf")
        val buf2 = ByteBuffer.allocate(buf.remaining())
        _log_ByteBuffer(buf2, "buf2")
        buf2.put(buf)
        _log_ByteBuffer(buf2, "buf2")
        buf2.rewind()
        _log_ByteBuffer(buf2, "buf2")
    }

    private fun _log_ByteBuffer(buf: ByteBuffer, name: String) {
        LogEx.d("${buf.position()}:${buf.limit()}:${buf.capacity()} $name[${String(buf.array())}]")
    }

    private val _ps_2_thread_loop = PublishSubject.create<Array<Any>>()

    fun _48_2_thread_loop() {
        _ps_2_thread_loop
            .observeOn(Schedulers.newThread())
            .map {
                val cmd = it[0] as String

                when (cmd) {
                    "cmd0" -> {
                        LogEx.d("cmd0 start")
                        runBlocking { delay(30) }
                        LogEx.d("cmd0 fin")
                        return@map arrayOf("onnext", arrayOf("cmd0"))
                    }
                    "interrupt0" -> {
                        LogEx.d("interrupt0")
                        runBlocking { delay(30) }
                    }
                }
                return@map it
            }
            .observeOn(Schedulers.newThread())
            .map {
                val cmd = it[0] as String

                when (cmd) {
                    "cmd1" -> {
                        LogEx.d("cmd1 start")
                        runBlocking { delay(10_000) }
                        LogEx.d("cmd1 fin")
                        return@map arrayOf("onnext", arrayOf("cmd1"))
                    }
                    "interrupt1" -> {
                        LogEx.d("interrupt1")
                        runBlocking { delay(10_000) }
                    }
                }
                return@map it
            }
            .observeOn(Schedulers.newThread())
            .map {
                val cmd = it[0] as String
                LogEx.d("onnext start cmd[${cmd}]")

                when (cmd) {
                    "onnext" -> {
                        val onnextArray = it[1] as Array<Any>
                        val incmd = onnextArray[0] as String
                        LogEx.d("onnext incmd[${incmd}]")
                        _ps_2_thread_loop.onNext(onnextArray)
                    }
                }

                LogEx.d("onnext fin cmd[${cmd}]")
                return@map it
            }
            .subscribe()


        repeat(100) {
            _ps_2_thread_loop.onNext(arrayOf("cmd0"))
            _ps_2_thread_loop.onNext(arrayOf("cmd1"))
        }
    }

    fun _49_2_thread_loop_make_interrupt0() {
        _ps_2_thread_loop.onNext(arrayOf("interrupt0"))
    }

    fun _50_2_thread_loop_make_interrupt1() {
        _ps_2_thread_loop.onNext(arrayOf("interrupt1"))
    }

    fun _51_pipeline_branch() {
        val ps_head = PublishSubject.create<String>()
        val ps_branch_0 = PublishSubject.create<String>()
        val ps_branch_1 = PublishSubject.create<String>()

        ps_head
            .observeOn(Schedulers.io())
            .map {
                val cmd = it as String
                LogEx.d("ps_head start $cmd")

                when(cmd) {
                    "cmd0" -> {
                        ps_branch_0.onNext("cmd0")
                    }
                    "cmd1" -> {
                        ps_branch_1.onNext("cmd1")
                    }
                }

                LogEx.d("ps_head fin $cmd")
                return@map it
            }
            .subscribe()

        ps_branch_0
            .observeOn(Schedulers.io())
            .map {
                val cmd = it as String
                LogEx.d("ps_branch_0 start $cmd")

                when(cmd) {
                    "cmd0" -> {
                        runBlocking { delay(1_000) }
                        ps_head.onNext("cmd0")
                    }
                }

                LogEx.d("ps_branch_0 fin $cmd")
                return@map it
            }
            .subscribe()

        ps_branch_1
            .observeOn(Schedulers.io())
            .map {
                val cmd = it as String
                LogEx.d("ps_branch_1 start $cmd")

                when(cmd) {
                    "cmd1" -> {
                        runBlocking { delay(3_000) }
                        ps_head.onNext("cmd1")
                    }
                }

                LogEx.d("ps_branch_1 fin $cmd")
                return@map it
            }
            .subscribe()

        ps_head.onNext("cmd0")
        ps_head.onNext("cmd1")
    }
}