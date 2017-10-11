<template>
    <div>
        <div class="alert alert-info" role="alert">工作卡總量: {{ displayDozenStr(quantity) }}</div>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-lg-1 control-label">優:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.good"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">副:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.sub"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">副未包:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.subNotPack"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">汙:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.stain"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">長短:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.longShort"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">破:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.broken"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">不均:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.notEven"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">油:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.oil"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">襪頭:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.head"></div>
            </div>
            <div class="form-group">
                <label class="col-lg-1 control-label">工號:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="myCard.operator"></div>
            </div>
            <div class="alert alert-info" role="alert">若該站已經是流動工作卡最後一站, 請選擇"結束工作卡"</div>
            <div class="form-group">
                <div class="col-lg-offset-1 col-lg-1">
                    <button class='btn btn-primary' @click='update'>更新</button>
                </div>
                <div class="col-lg-1">
                    <button class='btn btn-primary' @click='close'>結束工作卡</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style>

</style>
<script>
import axios from 'axios'
import { fromDozenStr, toDozenStr } from '../dozenExp'

export default {
    props: {
        tidyCard: {
            type: Object,
            required: true
        },
        quantity: {
            type: Number,
            required: true
        }
    },
    watch: {
        tidyCard(newTidyCard) {
            this.myCard.good = toDozenStr(newTidyCard.good),
            this.myCard.sub = toDozenStr(newTidyCard.sub),
            this.myCard.subNotPack = toDozenStr(newTidyCard.subNotPack),
            this.myCard.stain = toDozenStr(newTidyCard.stain),
            this.myCard.longShort = toDozenStr(newTidyCard.longShort),
            this.myCard.broken = toDozenStr(newTidyCard.broken),
            this.myCard.oil = toDozenStr(newTidyCard.oil),
            this.myCard.notEven = toDozenStr(newTidyCard.notEven),
            this.myCard.head = toDozenStr(newTidyCard.head),
            this.myCard.operator = newTidyCard.operator            
        }
    },
    data() {
        return {
            myCard: {
                good: toDozenStr(this.tidyCard.good),
                sub: toDozenStr(this.tidyCard.sub),
                subNotPack: toDozenStr(this.tidyCard.subNotPack),
                stain: toDozenStr(this.tidyCard.stain),
                longShort: toDozenStr(this.tidyCard.longShort),
                broken: toDozenStr(this.tidyCard.broken),
                oil: toDozenStr(this.tidyCard.oil),
                notEven: toDozenStr(this.tidyCard.notEven),
                head: toDozenStr(this.tidyCard.head),
                operator: this.tidyCard.operator
            }
        }
    },
    methods: {
        prepareTidyCard() {
            this.tidyCard.good = fromDozenStr(this.myCard.good)
            if (this.tidyCard.good == null) {
                alert("優不能是空白")
                return false
            }

            this.tidyCard.sub = fromDozenStr(this.myCard.sub)
            this.tidyCard.subNotPack = fromDozenStr(this.myCard.subNotPack)
            this.tidyCard.stain = fromDozenStr(this.myCard.stain)
            this.tidyCard.longShort = fromDozenStr(this.myCard.longShort)
            this.tidyCard.broken = fromDozenStr(this.myCard.broken)
            this.tidyCard.oil = fromDozenStr(this.myCard.oil)
            this.tidyCard.notEven = fromDozenStr(this.myCard.notEven)
            this.tidyCard.head = fromDozenStr(this.myCard.head)
            this.tidyCard.operator = this.myCard.operator

            if (this.tidyCard.operator == null || this.tidyCard.operator == "") {
                alert("工號不可是空白")
                return false
            }

            //FIXME
            let total = this.tidyCard.good + this.tidyCard.sub + this.tidyCard.stain + this.tidyCard.broken + this.tidyCard.notEven
            if (total > this.quantity * 1.1) {
                alert("數量超過總量!")
                return false
            }

            return true
        },
        displayDozenStr(v) {
            return toDozenStr(v)
        },
        update() {
            if (!this.prepareTidyCard())
                return

            axios.post("/TidyCard", this.tidyCard).then((resp) => {
                const ret = resp.data
                if (ret.ok) {
                    alert("成功")
                    this.$emit('updated')
                } else
                    alert("失敗:" + ret.msg)
            }).catch((err) => {
                alert(err)
            })
        },
        close() {
            if (!this.prepareTidyCard())
                return

            axios.post("/FinalTidyCard", this.tidyCard).then((resp) => {
                const ret = resp.data
                if (ret.ok) {
                    alert("成功")
                    this.$emit('updated')
                } else
                    alert("失敗:" + ret.msg)
            }).catch((err) => {
                alert(err)
            })
        }
    },
    components: {}
}
</script>
