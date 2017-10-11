<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group"><label class="col-lg-1 control-label">漂染單編號:</label>
                <div class="col-lg-4"><input type="text" placeholder="漂染單編號"
                                             class="form-control"
                                             v-model="queryParam._id">
                </div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">顏色:</label>
                <div class="col-lg-4"><input type="text" placeholder="顏色"
                                             class="form-control"
                                             v-model="queryParam.color">
                </div>
            </div>
            <div class="form-group"><label class="col-lg-1 control-label">漂染日期從:</label>
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
        <div v-if='display'>
            <dye-card-list url="/QueryDyeCard" :param="queryParam"></dye-card-list>
        </div>
    </div>
</template>
<style>
    body {
        background-color: #ff0000;
    }
</style>
<script>
    import axios from 'axios'
    import moment from 'moment'
    import Datepicker from 'vuejs-datepicker'
    import DyeCardList from "./DyeCardList.vue"
    import cardHelper from '../cardHelper'

    export default{
        data(){
            return {
                display: false,
                cardList: [],
                queryParam: {}
            }
        },
        computed: {
            start: {
                get: function () {
                    if (this.queryParam.start)
                        return moment(this.queryParam.start).toDate()
                    else
                        return null

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
                    else
                        return null
                },
                // setter
                set: function (newValue) {
                    this.queryParam.end = newValue.getTime()
                }
            }
        },
        methods: {
            prepareParam(){
                if (this.queryParam._id == "")
                    this.queryParam._id = null

                if (this.queryParam.color == '')
                    this.queryParam.color = null
            },
            query(){
                this.prepareParam()
                if (!this.display)
                    this.display = true

                this.queryParam = Object.assign({}, this.queryParam)
            }
        },
        components: {
            Datepicker,
            DyeCardList
        }
    }
</script>
