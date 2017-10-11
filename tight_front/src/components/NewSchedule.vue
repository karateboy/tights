<template>
    <div v-if="myDyeCardSpecList.length != 0">
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-lg-1 control-label">顏色:</label>
                <div class="col-lg-11">
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary dim" v-for="(spec, idx) in myDyeCardSpecList"
                               @click="setActiveSpec(spec)">
                            <input type="radio">{{ spec.color }} 交期:{{ displayDate(spec.due)}}</label>
                    </div>
                </div>
            </div>
            <div v-if="activeSpec">
                <div class="alert alert-info" role="alert">
                    條碼可不輸入, 由系統自行產生
                </div>
                <div class="form-group">
                    <label class="col-lg-1 control-label">訂單需求:</label>
                    <div class="col-lg-11">
                        <table class="table table-bordered">
                            <thead>
                            <tr>
                                <th></th>
                                <th>訂單編號</th>
                                <th>預定交貨日</th>
                                <th>工廠代號</th>
                                <th>尺寸</th>
                                <th>已排定產量/需求數量 (打)</th>
                                <th>排定產量 (打)</th>
                                <th>條碼</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(workSpec, idx) in activeSpec.workCardSpecList">
                                <td>
                                    <button class='btn btn-primary' @click='addWorkCard(workSpec)'><i class="fa fa-plus"
                                                                                                      aria-hidden="true"></i>&nbsp;新增流動卡
                                    </button>
                                </td>
                                <td>{{workSpec.orderId}}</td>
                                <td>{{displayDate(workSpec.due)}}</td>
                                <td>{{workSpec.factoryId}}</td>
                                <td>{{workSpec.detail.size}}</td>
                                <td>{{workCardTotalQuantity(workSpec) + "/" + displayQuantity(workSpec.need)}}</td>
                                <td><input type="text" v-model="workSpec.toProduce"></td>
                                <td><input type="text" v-model="workSpec.barcode"></td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="form-group">
                    <label class="col-lg-1 control-label">流動卡:</label>
                    <div class="col-lg-11">
                        <table class="table table-bordered">
                            <thead>
                            <tr>
                                <th></th>
                                <th>訂單編號</th>
                                <th>工廠代號</th>
                                <th>尺寸</th>
                                <th>生產數量(打)</th>
                                <th>條碼</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(workCard, idx) in workCards">
                                <td>
                                    <button class='btn btn-danger' @click='deleteWorkCard(idx)'><i class="fa fa-trash"
                                                                                                   aria-hidden="true"></i>&nbsp;刪除
                                    </button>
                                </td>
                                <td>{{workCard.orderId}}</td>
                                <td>{{workCard.workCardSpec.factoryId}}</td>
                                <td>{{workCard.workCardSpec.detail.size}}</td>
                                <td>{{displayQuantity(workCard.quantity)}}</td>
                                <td>{{barcode(workCard._id)}}</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="form-group">
                    <label class="col-lg-1 control-label">備註: (重染, 改染, 清洗...)</label>
                    <div class="col-lg-11">
                        <input type="text" v-model='remark'>
                    </div>
                </div>
                <div class="form-group">
                    <button class="col-lg-offset-1 btn btn-primary" @click="schedule" :disabled='!readyToSchedule'>
                        排定生產
                    </button>
                </div>
            </div>
        </div>
    </div>
    <div v-else>
        <div class="alert alert-info" role="alert">
            <strong>沒有可以排定的工作</strong>
        </div>
    </div>

</template>
<style>

</style>
<script>
    import axios from 'axios'
    import moment from 'moment'
    import * as dozenExp from '../dozenExp'

    export default{
        data(){
            return {
                dyeCardSpecList: [],
                fetched: false,
                activeSpec: null,
                workCards: [],
                remark: ""
            }
        },
        computed: {
            myDyeCardSpecList(){
                if (!this.fetched) {
                    axios.get("/DyeCardSpec").then(
                        (resp) => {
                            const ret = resp.data
                            const len = this.dyeCardSpecList.length
                            this.dyeCardSpecList.splice(0, len)
                            for (let v of ret) {
                                this.dyeCardSpecList.push(v)
                            }
                        }
                    ).catch((err) => {
                        console.log(err)
                    })
                    this.fetched = true
                    return this.dyeCardSpecList
                } else
                    return this.dyeCardSpecList
            },
            readyToSchedule(){
                return this.workCards.length > 0
            }
        },
        methods: {
            displayDate(millis){
                const mm = moment(millis).locale("zh_tw")
                const dateStr = mm.format('YYYY-MM-DD')
                const afterStr = mm.fromNow()
                return dateStr + " (" + afterStr + ")";
            },
            setActiveSpec(spec){
                this.activeSpec = spec
                /*
                 for (let workSpec of this.activeSpec.workCardSpecList) {
                 //workSpec.toProduce = 50
                 }
                 */
            },
            addWorkCard(workCardSpec){
                let quantity = parseInt(dozenExp.fromDozenStr(workCardSpec.toProduce))
                let workCard = {
                    _id: "",
                    orderId: workCardSpec.orderId,
                    detailIndex: workCardSpec.index,
                    quantity,
                    good: quantity,
                    active: true,
                    workCardSpec
                }
                if (workCardSpec.barcode)
                    workCard._id = workCardSpec.barcode

                this.workCards.push(workCard)
            },
            workCardTotalQuantity(workCardSpec){
                let total = 0
                for (let workCard of this.workCards) {
                    if (workCard.workCardSpec == workCardSpec) {
                        total += workCard.quantity
                    }
                }
                return dozenExp.toDozenStr(total)
            },
            barcode(id){
                if (id == "")
                    return "系統自動產生"
                else
                    return id
            },
            deleteWorkCard(idx){
                this.workCards.splice(idx, 1)
            },
            displayQuantity(quantity){
                return dozenExp.toDozenStr(quantity)
            },
            schedule(){
                const dyeCard = {
                    _id: "",
                    workIdList: [],
                    color: this.activeSpec.color,
                    active: true,
                    remark: this.remark
                }

                const url = "/ScheduleDyeWork"
                axios.post(url, {dyeCard, workCards: this.workCards}).then(
                    (resp) => {
                        const ret = resp.data
                        if (ret.ok) {
                            alert("成功")
                            this.$router.push({name: 'ActiveDyeCardList'})
                        }
                    }
                ).catch(
                    (err) => {
                        alert(err)
                    }
                )
            }
        },
        components: {}
    }
</script>
