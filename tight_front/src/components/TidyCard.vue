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
            <div class="alert alert-info" role="alert">更新實際使用的庫存</div>
            <div class="form-group">
                <label class="col-lg-1 control-label">庫存:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="inventoryStr"></div>
            </div>
            <div class="form-group">
                <div v-show="displayUpdateBtn" class="col-lg-offset-1 col-lg-1">
                    <button class='btn btn-primary' @click='update'>更新</button>
                </div>
                <div v-show="displayFinishBtn" class="col-lg-1">
                    <button class='btn btn-primary' @click='close'>更新並結束工作卡</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style>
</style>
<script>
import axios from "axios";
import { fromDozenStr, toDozenStr } from "../dozenExp";

export default {
  props: {
    tidyCard: {
      type: Object,
      required: true
    },
    quantity: {
      type: Number,
      required: true
    },
    inventory: {
      type: Number,
      required: true
    }
  },
  watch: {
    tidyCard(newTidyCard) {
      (this.myCard.good = toDozenStr(newTidyCard.good)),
        (this.myCard.sub = toDozenStr(newTidyCard.sub)),
        (this.myCard.subNotPack = toDozenStr(newTidyCard.subNotPack)),
        (this.myCard.stain = toDozenStr(newTidyCard.stain)),
        (this.myCard.longShort = toDozenStr(newTidyCard.longShort)),
        (this.myCard.broken = toDozenStr(newTidyCard.broken)),
        (this.myCard.oil = toDozenStr(newTidyCard.oil)),
        (this.myCard.notEven = toDozenStr(newTidyCard.notEven)),
        (this.myCard.head = toDozenStr(newTidyCard.head)),
        (this.myCard.operator = newTidyCard.operator);
    }
  },
  data() {
    console.log(this.inventory);
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
      },
      inventoryStr: toDozenStr(this.inventory)
    };
  },
  computed: {
    displayUpdateBtn() {
      if (
        this.tidyCard._id.phase === "檢襪" ||
        this.tidyCard._id.phase === "車洗標" ||
        this.tidyCard._id.phase === "剪線頭"
      )
        return true;
      else return false;
    },
    displayFinishBtn() {
      if (
        this.tidyCard._id.phase === "檢襪" ||
        this.tidyCard._id.phase === "車洗標" ||
        this.tidyCard._id.phase === "剪線頭"
      )
        return false;
      else return true;
    }
  },
  methods: {
    prepareTidyCard() {
      this.tidyCard.good = fromDozenStr(this.myCard.good);
      if (this.tidyCard.good == null) {
        alert("優不能是空白");
        return false;
      }

      this.tidyCard.sub = fromDozenStr(this.myCard.sub);
      this.tidyCard.subNotPack = fromDozenStr(this.myCard.subNotPack);
      this.tidyCard.stain = fromDozenStr(this.myCard.stain);
      this.tidyCard.longShort = fromDozenStr(this.myCard.longShort);
      this.tidyCard.broken = fromDozenStr(this.myCard.broken);
      this.tidyCard.oil = fromDozenStr(this.myCard.oil);
      this.tidyCard.notEven = fromDozenStr(this.myCard.notEven);
      this.tidyCard.head = fromDozenStr(this.myCard.head);
      this.tidyCard.operator = this.myCard.operator;

      if (this.tidyCard.operator == null || this.tidyCard.operator == "") {
        alert("工號不可是空白");
        return false;
      }

      //FIXME
      let total =
        this.tidyCard.good +
        this.tidyCard.sub +
        this.tidyCard.stain +
        this.tidyCard.broken +
        this.tidyCard.notEven;
      this.quantity = total;

      return true;
    },
    displayDozenStr(v) {
      return toDozenStr(v);
    },
    update() {
      if (!this.prepareTidyCard()) return;

      let inventory = fromDozenStr(this.inventoryStr);
      axios
        .post("/TidyCard", {
          tidyCard: this.tidyCard,
          inventory,
          quantity: this.quantity
        })
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功");
            this.$emit("updated");
          } else alert("失敗:" + ret.msg);
        })
        .catch(err => {
          alert(err);
        });
    },
    close() {
      if (!this.prepareTidyCard()) return;

      let inventory = fromDozenStr(this.inventoryStr);
      axios
        .post("/FinalTidyCard", {
          tidyCard: this.tidyCard,
          inventory
        })
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功");
            this.$emit("updated");
          } else alert("失敗:" + ret.msg);
        })
        .catch(err => {
          alert(err);
        });
    }
  },
  components: {}
};
</script>
