<template>
    <div class="ibox-content">
        <h1>成品倉庫課</h1>
        <br>
        <div class="ibox-content">
        <form class="form-horizontal" @submit.prevent="query">
            <div class="form-group has-feedback"><label class="col-lg-3 control-label">流動工作卡號:</label>
                <div class="col-lg-5"><input type="text" placeholder="掃描條碼" autofocus
                                             class="form-control"
                                             v-model="workCardID">
                </div>
            </div>
        </form>  
        <hr>      
        <div v-if="display" class="form-horizontal">
            <div class="form-group">
                <label class="col-lg-1 control-label">庫存:</label>
                <div class="col-lg-2"><input type="text" class="form-control" v-model="inventoryStr"></div>
            </div>
            <div class="form-group">
                <div class="col-lg-1">
                    <button class='btn btn-primary' @click='close' :disabled="!workCard.active">結束工作卡</button>
                </div>
            </div>
        </div>
        </div>
    </div>
</template>
<style>
body {
  background-color: #ff0000;
}
</style>
<script>
import axios from "axios";
import { fromDozenStr, toDozenStr } from "../dozenExp";

export default {
  data() {
    return {
      workCardID: "",
      workCard: {},
      inventoryStr: "",
      display: false
    };
  },
  methods: {
    query() {
      this.display = false;
      axios
        .get("/WorkCard/" + this.workCardID)
        .then(resp => {
          const ret = resp.data;
          this.workCard = ret;
          this.display = true;
          this.inventoryStr = toDozenStr(this.workCard.inventory);
        })
        .catch(error => {
          if (error.response) {
            alert(error.response.data);
          } else {
            alert(error.message);
          }
        });
    },
    close() {
      this.workCard.inventory = fromDozenStr(this.inventoryStr);
      this.workCard.active = false;

      axios
        .post("/WorkCard", this.workCard)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功");
          } else alert("失敗:" + ret.msg);
          
          this.display = false;
          this.workCardID = "";
        })
        .catch(err => {
          alert(err);
        });
    }
  },
  components: {}
};
</script>
