<template>
    <div>
        <div class="alert alert-info" role="alert">工作卡總量: {{displayQuantity() + '打'}}</div>
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
            <div class="alert alert-info" role="alert">多人參與請用逗號分隔, 如: 122,85</div>
            <div class="form-group">
                <label class="col-lg-1 control-label">工號:</label>
                <div class="col-lg-5"><input type="text" class="form-control" v-model="myCard.operator"></div>
            </div>
            <div class="form-group">
                <div class="col-lg-offset-1 col-lg-1">
                    <button class='btn btn-primary' @click='update'>更新</button>
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
        stylingCard: {
            type: Object,
            required: true
        },
        workCardID: {
            type: String,
            required: true
        },
        quantity: {
            type: Number,
            required: true
        }
    },
    watch: {
        stylingCard(newCard) {
            this.myCard.good = toDozenStr(newCard.good),
                this.myCard.sub = toDozenStr(newCard.sub),
                this.myCard.subNotPack = toDozenStr(newCard.subNotPack),
                this.myCard.stain = toDozenStr(newCard.stain),
                this.myCard.longShort = toDozenStr(newCard.longShort),
                this.myCard.broken = toDozenStr(newCard.broken),
                this.myCard.oil = toDozenStr(newCard.oil),
                this.myCard.notEven = toDozenStr(newCard.notEven),
                this.myCard.head = toDozenStr(newCard.head),
                this.myCard.operator = newCard.operator.join()
        }
    },
    data() {
        return {
            myCard: {
                good: toDozenStr(this.stylingCard.good),
                sub: toDozenStr(this.stylingCard.sub),
                subNotPack: toDozenStr(this.stylingCard.subNotPack),
                stain: toDozenStr(this.stylingCard.stain),
                longShort: toDozenStr(this.stylingCard.longShort),
                broken: toDozenStr(this.stylingCard.broken),
                oil: toDozenStr(this.stylingCard.oil),
                notEven: toDozenStr(this.stylingCard.notEven),
                head: toDozenStr(this.stylingCard.head),
                operator: this.stylingCard.operator.join()
            }
        }
    },
    methods: {
        prepareStylingCard() {
            this.stylingCard.good = fromDozenStr(this.myCard.good)
            if (this.stylingCard.good == null) {
                alert("優不能是空白")
                return false
            }

            this.stylingCard.sub = fromDozenStr(this.myCard.sub)
            this.stylingCard.subNotPack = fromDozenStr(this.myCard.subNotPack)
            this.stylingCard.stain = fromDozenStr(this.myCard.stain)
            this.stylingCard.longShort = fromDozenStr(this.myCard.longShort)
            this.stylingCard.broken = fromDozenStr(this.myCard.broken)
            this.stylingCard.oil = fromDozenStr(this.myCard.oil)
            this.stylingCard.notEven = fromDozenStr(this.myCard.notEven)
            this.stylingCard.head = fromDozenStr(this.myCard.head)
            this.stylingCard.operator = this.myCard.operator.trim().split("[,.]")

            if (this.stylingCard.operator == null || this.stylingCard.operator.length == 0) {
                alert("工號不能是空白")
                return false
            }

            let total = this.stylingCard.good + this.stylingCard.sub + this.stylingCard.stain + this.stylingCard.broken + this.stylingCard.notEven
            if (total > this.quantity * 1.1) {
                alert("超過總量")
                return false
            }
            return true
        },
        update() {
            if (!this.prepareStylingCard())
                return

            axios.post("/StylingCard/" + this.workCardID, this.stylingCard).then((resp) => {
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
        displayQuantity() {
            return toDozenStr(this.quantity)
        }
    },
    components: {}
}
</script>
