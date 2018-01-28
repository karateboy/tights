<template>
    <div>
        <br>
        <div class="form-horizontal">
          <div class="alert alert-info" role="alert">刪除已經不用的顏色不會影響已經開出的流動卡</div>
          <div class="form-group"><label class="col-lg-1 control-label">顏色:</label>
            <div class="col-lg-11">
              <div data-toggle="buttons-checkbox" class="btn-group">
                <label class="btn btn-primary" v-for="color in colorList" :key="color" :for="color">
                  <input type="checkbox" :id="color" :value="color" v-model="selectedColor"> {{ color }}</label>                            
              </div> 
            </div>
          </div>
          <div class="form-group">
            <div class="col-lg-offset-1 col-lg-1">
              <button class="btn btn-primary"
                @click.prevent="deleteColor">刪除
              </button>
            </div>
          </div>
        </div>
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
  data() {
    return {
      color: "",
      colorList: [],
      selectedColor: []
    };
  },
  mounted() {
    this.getColor();
  },
  computed: {},
  methods: {
    getColor() {
      axios
        .get("/ColorSeq")
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            this.colorList.splice(0, this.colorList.length);
            for (let color of ret) {
              this.colorList.push(color);
            }
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    deleteColor() {
      console.log(this.selectedColor);
      let param = { colorSeq: this.selectedColor };
      let json = JSON.stringify(param);
      axios
        .delete(`/ColorSeq/${encodeURIComponent(json)}`)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功");
            this.getColor();
          }
        })
        .catch(err => alert(err));
    }
  },
  components: {}
};
</script>
