<template>
    <div>
        <br>
        <form class="form-horizontal" @submit.prevent="transferDyeCard">
            <div class="form-group has-feedback"><label class="col-lg-3 control-label">漂染單編號:</label>
                <div class="col-lg-5"><input type="text" placeholder="掃描條碼" autofocus
                                             class="form-control"
                                             v-model="id">
                </div>
            </div>
        </form>
    </div>
</template>
<style scoped>
body {
  background-color: #ff0000;
}
</style>
<script>
import axios from "axios";

export default {
  props: {
    dep: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      id: ""
    };
  },
  methods: {
    transferDyeCard() {
      let url = `/TransferDyeCard`;
      const param = {
        _id: this.id,
        dep: this.dep
      };

      axios
        .post(url, param)
        .then(resp => {
          console.log(resp);
          if (resp.status == 200) {
            alert("成功");
            this.cleanup();
          } else {
            alert("找不到漂染卡紀錄:" + resp.statusText);
            this.id = "";
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    cleanup() {
      this.id = "";
    }
  }
};
</script>
