<template>
    <div class="form-horizontal">
        <table class="table table-bordered">
            <tbody>
            <tr>
                <td>
                    <label class="control-label">染色人員:</label>
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary"
                               v-for="operator in operatorList"
                               @click="dyeCard.operator=operator"
                               :class="{active: dyeCard.operator==operator }">
                            <input type="radio">{{ operator }} </label>
                    </div>
                </td>
                <td>開始: {{displayTime(dyeCard.startTime)}}
                    <br>
                    結束: {{displayTime(dyeCard.endTime)}}
                    <br>
                    總時間: {{displayDiff(dyeCard.startTime, dyeCard.endTime)}}
                </td>
                <td>染鍋:
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary"
                               v-for="pot in potList"
                               @click="dyeCard.pot=pot"
                               :class="{active: dyeCard.pot==pot }">
                            <input type="radio">{{ pot }} </label>
                    </div>
                </td>
                <td>襪重:<input type="number" class="form-control" v-model.number="dyeCard.weight" :readonly="!edit"/></td>
            </tr>
            <tr>
                <td rowspan="2">精煉程序(kg)</td>
                <td rowspan="2">精煉劑:
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary"
                               v-for="refinePotion in refinePotionList"
                               @click="dyeCard.refineProcess.refinePotion=refinePotion"
                               :class="{active: dyeCard.refineProcess.refinePotion==refinePotion }">
                            <input type="radio">{{ refinePotion }} </label>
                    </div>
                    <div>kg:
                        <input type="number" class="form-control" v-model.number="dyeCard.refineProcess.refine"
                               :readonly="!edit"/>
                    </div>

                </td>
                <td rowspan="2">乳化劑(kg):<input type="number" class="form-control" v-model.number="dyeCard.refineProcess.milk"
                                               :readonly="!edit"/></td>
                <td>溫度(C):<input type="number" class="form-control" v-model.number="dyeCard.refineProcess.refineTime"
                                 :readonly="!edit"/></td>
            </tr>
            <tr>
                <td>時間(分):<input type="number" class="form-control" v-model.number="dyeCard.refineProcess.refineTemp"
                                 :readonly="!edit"/></td>
            </tr>

            <tr>
                <td rowspan="3">染色藥劑(g)</td>
                <td>Y:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.y" :readonly="!edit"/></td>
                <td>螢光劑:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.Fluorescent"
                               :readonly="!edit"/></td>
                <td rowspan="3">其他:<input type="text" class="form-control" v-model="dyeCard.dyePotion.otherDyeType"
                                          :readonly="!edit"/>
                    數量:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.otherDye"
                              :readonly="!edit"/>
                </td>
            </tr>
            <tr>
                <td>R:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.r" :readonly="!edit"/></td>
                <td>增白劑:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.Brightener"
                               :readonly="!edit"/></td>
            </tr>
            <tr>
                <td>B:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.b" :readonly="!edit"/></td>
                <td>黑色:<input type="number" class="form-control" v-model.number="dyeCard.dyePotion.black"
                               :readonly="!edit"/></td>
            </tr>
            <tr>
                <td rowspan="4">染色程序(kg)</td>
                <td>均染劑:
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary"
                               v-for="evenDyeType in evenDyeTypeList"
                               @click="dyeCard.dyeProcess.evenDyeType=evenDyeType"
                               :class="{active: dyeCard.dyeProcess.evenDyeType==evenDyeType }">
                            <input type="radio">{{ evenDyeType }} </label>
                    </div>
                    <div>
                        kg:
                        <input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.evenDye"
                               :readonly="!edit"/>
                    </div>
                </td>
                <td>冰醋酸:<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.iceV" :readonly="!edit"/>
                </td>
                <td>溫度<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.dyeTemp" :readonly="!edit"/>
                </td>
            </tr>
            <tr>
                <td>醋銨:<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.vNH3" :readonly="!edit"/>
                </td>
                <td></td>
                <td>時間(分鐘):<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.dyeTime"
                                  :readonly="!edit"/></td>
            </tr>
            <tr>
                <td>氨水:<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.nh3" :readonly="!edit"/>
                </td>
                <td></td>
                <td></td>
            </tr>
            <tr>
                <td>起染pH:<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.phStart"
                                :readonly="!edit"/></td>
                <td></td>
                <td>染終pH:<input type="number" class="form-control" v-model.number="dyeCard.dyeProcess.phEnd"
                                :readonly="!edit"/></td>
            </tr>
            <tr>
                <td rowspan="3">後處理程序(kg)</td>
                <td>固色劑:<input type="number" class="form-control" v-model.number="dyeCard.postProcess.fixedPotion"
                               :readonly="!edit"/></td>
                <td>陽離子柔軟劑:<input type="number" class="form-control" v-model.number="dyeCard.postProcess.postiveSoftener"
                                  :readonly="!edit"/></td>
                <td>溫度:<input type="number" class="form-control" v-model.number="dyeCard.postProcess.temp"
                              :readonly="!edit"/></td>
            </tr>
            <tr>
                <td>冰醋酸:<input type="number" class="form-control" v-model.number="dyeCard.postProcess.iceV" :readonly="!edit"/>
                </td>
                <td></td>
                <td>時間:<input type="number" class="form-control" v-model.number="dyeCard.postProcess.softenTime"
                              :readonly="!edit"/></td>
            </tr>
            <tr>
                <td>矽利康:<input type="number" class="form-control" v-model.number="dyeCard.postProcess.silicon"
                               :readonly="!edit"/></td>
                <td></td>
                <td></td>
            </tr>
            <tr>
                <td>烘乾</td>
                <td>溫度:<input type="number" class="form-control" v-model.number="dyeCard.dryTemp" :readonly="!edit"/></td>
                <td>時間:<input type="number" class="form-control" v-model.number="dyeCard.dryTime" :readonly="!edit"/></td>
                <td>機台:<input type="text" class="form-control" v-model="dyeCard.machine" :readonly="!edit"/></td>
            </tr>
            </tbody>
        </table>
        <hr>
        <table class="table table-bordered">
            <thead>
            <tr>
                <th>尺寸</th>
                <th>染前長度(cm)</th>
                <th>染後長度(cm)</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="sizeChart in dyeCard.sizeCharts">
                <td>{{sizeChart.size}}</td>
                <td><input type="number" class="form-control" v-model.number="sizeChart.before" :readonly="!edit"/></td>
                <td><input type="number" class="form-control" v-model.number="sizeChart.after" :readonly="!edit"/></td>
            </tr>
            </tbody>
        </table>
        <div class="form-group">
            <div v-if='edit' class="col-lg-offset-1">
                <button class="btn btn-primary" @click.prevent="update">更新
                </button>
            </div>
        </div>
    </div>
</template>
<style scoped>
    body{
        background-color:#ff0000;
    }







</style>
<script>
    import Datepicker from 'vuejs-datepicker'
    import axios from 'axios'
    import moment from  'moment'
    export default{
        props: {
            edit: {
                type: Boolean,
                default: true
            },
            dyeCard: {
                type: Object,
                required: true
            }
        },
        data(){
            return {
                potList: [
                    '美1', '美2', '美3', '美4', '泳1', '泳2', '泳3', '泳4', '泳5', '壓小', '壓中', '壓大', '義', '樣1', '樣2'
                ],
                refinePotionList: [
                    'LYS', '環保', '特用', '其他'
                ],
                evenDyeTypeList: ['一般', 'PAM'],
                operatorList: ['康志明', '劉守任', '詹鎮岳', '陳炳翔', '許君豪']
            }
        },
        computed: {
            dyeCardDate: {
                get: function () {
                    if (this.dyeCard.date)
                        return new Date(this.dyeCard.date)
                    else
                        return new Date()
                },
                // setter
                set: function (newValue) {
                    this.dyeCard.date = newValue.getTime()
                }
            }
        },
        methods: {
            prepareDyeCard(){
                /*
                if(this.dyeCard.weight)
                    this.dyeCard.weight = parseFloat(this.dyeCard.weight)

                if(this.dyeCard.refineProcess){
                    if(this.dyeCard.refineProcess.refine)
                        this.dyeCard.refineProcess.refine = parseFloat(this.dyeCard.refineProcess.refine)

                    if(this.dyeCard.refineProcess.milk)
                        this.dyeCard.refineProcess.milk = parseFloat(this.dyeCard.refineProcess.milk)

                    if(this.dyeCard.refineProcess.refineTime)
                        this.dyeCard.refineProcess.refineTime = parseFloat(this.dyeCard.refineProcess.refineTime)

                    if(this.dyeCard.refineProcess.refineTemp)
                        this.dyeCard.refineProcess.refineTemp = parseFloat(this.dyeCard.refineProcess.refineTemp)
                }

                if(this.dyeCard.dyePotion){
                    if(this.dyeCard.dyePotion.y)
                        this.dyeCard.dyePotion.y = parseFloat(this.dyeCard.dyePotion.y)
                }
                */
            },
            update(){
                this.prepareDyeCard()
                axios.post("/DyeCard", this.dyeCard).then((resp) => {
                    const ret = resp.data
                    if (ret.ok) {
                        alert("成功")
                        this.$emit('updated')
                    } else
                        alert('失敗')
                }).catch((err) => {
                    alert(err)
                })
            },
            displayTime(v){
                return moment(v).format('YYYY-MM-DD hh:mm');
            },
            displayDiff(a, b){
                var start = moment(a);
                var end = moment(b);
                return end.diff(start, 'minutes') + "分鐘"
            }
        },
        components: {
            Datepicker
        }
    }
</script>
