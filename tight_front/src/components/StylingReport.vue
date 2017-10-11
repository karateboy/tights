<template>
    <div>
        <div class="form-horizontal">
            <div class="form-group"><label class="col-lg-1 control-label">XX日期從:</label>
                <div class="col-lg-5">
                    <div class="input-daterange input-group">
                        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
                        <datepicker v-model="start" language="zh"
                                    format="yyyy-MM-dd"></datepicker>
                    </div>
                </div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">至(不含):</label>
                <div class="col-lg-5">
                    <div class="input-daterange input-group">
                        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
                        <datepicker v-model="end" language="zh"
                                    format="yyyy-MM-dd"></datepicker>
                    </div>
                </div>
            </div>
            <div class="form-group">
                <div class="col-lg-offset-1">
                    <button class='btn btn-primary' @click='query'>查詢</button>
                </div>
            </div>
        </div>
        <div v-if='showReport'>
            <label class="btn btn-outline" data-toggle="tooltip" data-placement="bottom" title="Excel"><a @click.prevent='downloadExcel'><i class="fa fa-file-excel-o fa-2x"></i></a></label>
            <table class="table  table-bordered table-condensed">
                <thead>
                <tr class='info'>
                    <th>日期</th>
                    <th>流動卡編號</th>
                    <th>客戶編碼</th>
                    <th>工廠代碼</th>
                    <th>顏色</th>
                    <th>尺寸</th>
                    <th>優</th>
                    <th>副</th>
                    <th>汙</th>
                    <th>破</th>
                    <th>不均</th>
                    <th>工號</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for='card in cardList'>
                    <td>{{displayDate(card.stylingCard.date)}}</td>
                    <td>{{card._id}}</td>
                    <td>{{displayCustomerID(card)}}</td>
                    <td>{{displayFactoryID(card)}}</td>
                    <td>{{displayColor(card)}}</td>
                    <td>{{displaySize(card)}}</td>
                    <td>{{displayQuantity(card.stylingCard.good)}}</td>
                    <td>{{displayQuantity(card.stylingCard.sub)}}</td>
                    <td>{{displayQuantity(card.stylingCard.stain)}}</td>
                    <td>{{displayQuantity(card.stylingCard.broken)}}</td>
                    <td>{{displayQuantity(card.stylingCard.notEven)}}</td>
                    <td>{{card.stylingCard.operator.join()}}</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>
<style>
    body{
        background-color:#ff0000;
    }
</style>
<script>
    import axios from 'axios'
    import moment from 'moment'
    import Datepicker from 'vuejs-datepicker'
    import * as dozenExp from '../dozenExp'
    import baseUrl from '../baseUrl'
    import cardHelper from '../cardHelper'

    export default{
        data(){
            return{
                queryParam: {},
                showReport: false,
                cardList:[],
                operatorList:[]
            }
        },
        computed: {
            start: {
                get: function () {
                    if (this.queryParam.start)
                        return moment(this.queryParam.start).toDate()
                    else{
                        const start = moment("0", "hh").toDate()
                        this.queryParam.start = start.getTime()
                        return start;
                    }

                },
                // setter
                set: function (newValue) {
                    this.queryParam.start = newValue.getTime()
                }
            },
            end: {
                get: function () {
                    if (this.queryParam.end)
                        return moment(this.queryParam.end).toDate()
                    else{
                        const end = moment("0", "hh").add(1, 'day').toDate()
                        this.queryParam.end = end.getTime()
                        return end
                    }
                },
                // setter
                set: function (newValue) {
                    this.queryParam.end = newValue.getTime()
                }
            }
        },
        methods: {
            query(){
                const url = '/StylingReport/'+this.queryParam.start+'/'+ this.queryParam.end
                axios.get(url).then((resp) => {
                    const ret = resp.data
                    this.cardList.splice(0, this.cardList.length)
                    for(let card of ret.cards){
                        cardHelper.populateWorkCard(card)
                        this.cardList.push(card)
                    }
                    this.operatorList.splice(0, this.operatorList.length)
                    for(let operator of ret.operatorList){
                        this.operatorList.push(operator)
                    }
                    this.showReport = true
                }).catch((err) => {
                    alert(err)
                })
            },
            displayCustomerID(workCard){
                if(workCard.order)
                    return workCard.order.customerId
                else
                    return "查詢中"
            },
            displayFactoryID(workCard){
                if(workCard.order)
                    return workCard.order.factoryId
                else
                    return "查詢中"
            },
            displayColor(workCard){
                if(workCard.order && workCard.order.details){
                    return workCard.order.details[workCard.detailIndex].color
                }else
                    return "查詢中"
            },
            displaySize(workCard){
                if(workCard.order && workCard.order.details){
                    return workCard.order.details[workCard.detailIndex].size
                }else
                    return "查詢中"
            },
            displayDate(mm){
                return moment(mm).format('YYYY-MM-DD')
            },
            displayQuantity(v){
                return dozenExp.toDozenStr(v)
            },
            downloadExcel(){
                const url = baseUrl() + '/StylingReport/Excel/'+this.queryParam.start+'/'+ this.queryParam.end
                window.open(url)
            }
        },
        components:{
            Datepicker
        }
    }
</script>
