<template>
    <div>
        <div v-if="orderList.length != 0">
            <table class="table table-hover table-bordered table-condensed">
                <thead>
                <tr class='info'>
                    <th></th>
                    <th>訂單編號</th>
                    <th>客戶編號</th>
                    <th>品名</th>
                    <th>訂單總數(打)</th>
                    <th>總完成打數</th>
                    <th>完成打數/訂單總數(%)</th>
                    <th>預定出貨日</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(order, index) in orderList" :class='{success: selectedIndex == index}'>
                    <td>
                        <button class="btn btn-primary" @click="displayOrder(index)"><i class="fa fa-eye"></i>&nbsp;內容
                        </button>
                        <button class="btn btn-primary" @click="prepareCloneOrder(index)"><i class="fa fa fa-clone"></i>&nbsp;複製
                        </button>
                        <button class="btn btn-primary" @click='displayProgress(index)'><i class="fa fa-truck"
                                                                                           aria-hidden="true"></i>&nbsp;進度
                        </button>
                        <button class="btn btn-info" @click='getPdf(index)'><i class="fa fa-pdf" aria-hidden="true"></i>&nbsp;列印
                        </button>
                        <button class="btn btn-success" @click="closeOrder(index)" v-if='order.active'><i
                                class="fa fa-money"></i>&nbsp;結案
                        </button>
                        <button class="btn btn-danger" @click="deleteOrder(index)" v-if='order.active'><i
                                class="fa fa-trash"></i>&nbsp;刪除
                        </button>
                        <button class="btn btn-danger" @click="reopenOrder(index)" v-if='!order.active'><i
                                class="fa fa-repeat"></i>&nbsp;重啟
                        </button>
                    </td>
                    <td>{{ order._id}}</td>
                    <td>{{ order.customerId}}</td>
                    <td>{{ order.name}}</td>
                    <td>{{ displayQuantity(order)}}</td>
                    <td>{{ displayProduced(order)}}</td>
                    <td>
                        <div class="progress">
                            <div class="progress-bar progress-bar-success progress-bar-striped" role="progressbar"
                                 :aria-valuenow="productionPercent(order)" aria-valuemin="0" aria-valuemax="100"
                                 :style="{width:productionPercent(order)+'%'}">
                                {{productionPercent(order)}}%
                            </div>
                        </div>
                    </td>
                    <td>{{ displayDate(order.expectedDeliverDate) }}</td>
                </tr>
                </tbody>
            </table>
            <pagination for="orderList" :records="total" :per-page="5"
                        count-text="第{from}到第{to}筆/共{count}筆|{count} 筆|1筆"></pagination>
        </div>
        <div v-else class="alert alert-info" role="alert">沒有符合的訂單</div>
        <hr>
        <div v-if="display=='detail'">
            <order-detail></order-detail>
        </div>
        <div v-else-if="display=='progress'">
            <order-progress></order-progress>
        </div>
    </div>
</template>
<style scoped>
    body {
    }


</style>
<script>
    import {mapActions} from 'vuex'
    import OrderDetail from './OrderDetail.vue'
    import OrderProgress from './OrderProgress.vue'
    import moment from 'moment'
    import {Pagination, PaginationEvent} from 'vue-pagination-2'
    import baseUrl from '../baseUrl'
    import {toDozenStr} from '../dozenExp'
    import axios from 'axios'
    import cardHelper from '../cardHelper'
    export default{
        props: {
            url: {
                type: String,
                required: true
            },
            param: {
                type: Object
            }
        },
        data(){
            return {
                orderList: [],
                skip: 0,
                limit: 5,
                total: 1,
                display: '',
                selectedIndex: -1,
                order: {}
            }
        },
        mounted: function () {
            this.fetchOrder(this.skip, this.limit)
            PaginationEvent.$on('vue-pagination::orderList', this.handlePageChange)
        },
        watch: {
            url: function (newUrl) {
                console.log(newUrl)
                this.fetchOrder(this.skip, this.limit)
            },
            param: function (newParam) {
                console.log(newParam)
                this.fetchOrder(this.skip, this.limit)
            }
        },
        methods: {
            ...mapActions(['showOrder', 'cloneOrder']),
            processResp(resp){
                this.orderList.splice(0, this.orderList.length)
                for (let v of resp.data) {
                    cardHelper.getOrderProductionSummary(v)
                    this.orderList.push(v)
                }
            },
            fetchOrder(skip, limit){
                let request_url = `${this.url}/${skip}/${limit}`

                if (this.param) {
                    axios.post(request_url, this.param).then(this.processResp).catch((err) => {
                        alert(err)
                    })
                } else {
                    axios.get(request_url).then(this.processResp).catch((err) => {
                        alert(err)
                    })
                }
                this.fetchOrderCount()
            },
            fetchOrderCount(){
                let request_url = `${this.url}/count`
                if (this.param) {
                    axios.post(request_url, this.param).then(resp => {
                        this.total = resp.data
                    }).catch((err) => {
                        alert(err)
                    })
                } else {
                    axios.get(request_url).then(resp => {
                        this.total = resp.data
                    }).catch((err) => {
                        alert(err)
                    })
                }
            },
            handlePageChange(page){
                this.skip = (page - 1) * this.limit
                this.fetchOrder(this.skip, this.limit)
            },
            displayDate(millis){
                const mm = moment(millis)
                const dateStr = mm.format('YYYY-MM-DD')
                const afterStr = mm.fromNow()
                return dateStr + " (" + afterStr + ")";
            },
            displayOrder(idx){
                this.selectedIndex = idx
                this.showOrder(this.orderList[idx])
                this.display = 'detail';
            },
            prepareCloneOrder(idx){
                this.selectedIndex = idx
                this.cloneOrder(this.orderList[idx])
                this.$router.push({name: 'NewOrder'})
            },
            displayProgress(idx){
                this.selectedIndex = idx
                this.showOrder(this.orderList[idx])
                this.display = 'progress'
            },
            getPdf(idx){
                let url = baseUrl() + "/OrderPDF/" + this.orderList[idx]._id
                window.open(url)
            },
            closeOrder(idx){
                axios.post("/CloseOrder/" + this.orderList[idx]._id).then((resp) => {
                    const ret = resp.data
                    if (ret.ok) {
                        alert("訂單結案")
                        this.orderList[idx].active = false
                    } else {
                        alert(ret.msg)
                    }
                }).catch((err) => {
                    alert(err)
                })
            },
            reopenOrder(idx){
                axios.post("/ReopenOrder/" + this.orderList[idx]._id).then((resp) => {
                    const ret = resp.data
                    if (ret.ok) {
                        alert("訂單重啟")
                        this.orderList[idx].active = true
                    } else {
                        alert(ret.msg)
                    }
                }).catch((err) => {
                    alert(err)
                })
            },
            deleteOrder(idx){
                axios.delete("/Order/" + this.orderList[idx]._id).then((resp) => {
                    const ret = resp.data
                    if (ret.ok) {
                        alert("訂單刪除")
                        this.orderList.splice(idx, 1)
                    } else {
                        alert(ret.msg)
                    }
                }).catch((err) => {
                    alert(err)
                })
            },
            displayQuantity(order){
                let total = 0;
                for (let detail of order.details) {
                    total += detail.quantity
                }
                return toDozenStr(total)
            },
            displayProduced(order){
                if (order.productionSummary.length != 0) {
                    return toDozenStr(order.productionSummary[0].produced)
                } else {
                    return "查詢中"
                }
            },
            productionPercent(order){
                if (order.productionSummary.length != 0) {
                    let total = 0;
                    for (let detail of order.details) {
                        total += detail.quantity
                    }
                    let percent = order.productionSummary[0].produced * 100 / total
                    if (percent > 100)
                        percent = 100

                    return parseInt(percent)
                } else {
                    return 0
                }
            }
        },
        components: {
            OrderDetail,
            OrderProgress,
            Pagination
        }
    }
</script>
