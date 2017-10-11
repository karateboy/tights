<template>
    <div>
        <table class="table table-bordered">
            <thead>
            <tr>
                <th rowspan="2">流動卡編號</th>
                <th rowspan="2" class='text-center'>數量<br>優/預定(打)</th>
                <th rowspan="2" class='text-center'>漂染</th>
                <th rowspan="2">定型</th>
                <th colspan="5" class='text-center'>整理</th>
            </tr>
            <tr>
                <th>檢襪</th>
                <th>車洗標</th>
                <th>剪線頭</th>
                <th>整理包裝</th>
                <th>成品倉庫</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>{{workCard._id}}</td>
                <td class='text-right'>{{displayGoodQuantity(workCard)}}</td>
                <td class='text-right'>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true"
                       v-if='dyeCard.active'></i>
                    <i class="fa fa-check" style="color:green" aria-hidden="true" v-else></i>
                </td>
                <td class='text-right'>
                    <i class="fa fa-check" style="color:green" aria-hidden="true"
                       v-if='stylingReady(workCard.stylingCard)'>{{displayQuantity(workCard.stylingCard.good)}}</i>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true" v-else></i>
                </td>
                <td class='text-right'>
                    <i class="fa fa-check" aria-hidden="true" style="color:green" v-if='tidyMap[0]'>{{displayQuantity(tidyMap[0].good)}}</i>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true" v-else></i>
                </td>
                <td class='text-right'>
                    <i class="fa fa-check" aria-hidden="true" style="color:green" v-if='tidyMap[1]'>{{displayQuantity(tidyMap[1].good)}}</i>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true" v-else></i>
                </td>
                <td class='text-right'>
                    <i class="fa fa-check" aria-hidden="true" style="color:green" v-if='tidyMap[2]'>{{displayQuantity(tidyMap[2].good)}}</i>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true" v-else></i>
                </td>
                <td class='text-right'>
                    <i class="fa fa-check" aria-hidden="true" style="color:green" v-if='tidyMap[3]'>{{displayQuantity(tidyMap[3].good)}}</i>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true" v-else></i>
                </td>
                <td class='text-right'>
                    <i class="fa fa-check" aria-hidden="true" style="color:green" v-if='tidyMap[4]'>{{displayQuantity(tidyMap[4].good)}}</i>
                    <i class="fa fa-ban" style="color:red" aria-hidden="true" v-else></i>
                </td>
            </tr>
            </tbody>
        </table>

    </div>
</template>
<style>
</style>
<script>
    import Vue from 'vue'
    import axios from 'axios'
    import * as dozenExpr from '../dozenExp'
    export default{
        props: {
            workCard: {
                type: Object,
                required: true
            }
        },
        data(){
            return {
                workCardId:null,
                phaseMap:['檢襪','車洗標','剪線頭','整理包裝','成品倉庫'],
                tidyMap_:[],
                dyeCard: {}
            }
        },
        computed:{
            tidyMap(){
                if(this.workCardId != this.workCard._id){
                    this.populateWorkCard()
                    this.workCardId = this.workCard._id
                }
                return this.tidyMap_
            }
        },
        methods: {
            stylingReady(card){
                if (card) {
                    return true
                } else {
                    return false
                }
            },
            showDyeStatus(){
                if (this.dyeCard.active)
                    return 'X'
                else
                    return 'O'
            },
            populateWorkCard(){
                axios.get("/TidyCardList/" + this.workCard._id).then((resp) => {
                    const ret = resp.data
                    for(let i=0;i<5;i++)
                        Vue.set(this.tidyMap_, i, null)

                    for (let tidy of ret) {
                        Vue.set(this.tidyMap_, this.phaseMap.indexOf(tidy.phase), tidy)
                    }
                }).catch((err) => {
                    alert(err)
                })
                axios.get("/DyeCard/" + this.workCard.dyeCardID).then((resp) => {
                    this.dyeCard = resp.data
                }).catch((err) => {
                    alert(err)
                })
            },
            displayGoodQuantity(workCard){
                //workCard.good + "/" +workCard.quantity
                return dozenExpr.toDozenStr(workCard.good) + "/" + dozenExpr.toDozenStr(workCard.quantity)
            },
            displayQuantity(value){
                return dozenExpr.toDozenStr(value)
            }
        },
        components: {}
    }
</script>
