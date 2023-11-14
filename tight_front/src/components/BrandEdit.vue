<template>
    <div>
        <br>
        <div class="form-horizontal">
          <div class="form-group"><label class="col-lg-1 control-label">品牌:</label>
            <div class="col-lg-11">
              <div data-toggle="buttons-checkbox" class="btn-group">
                <label class="btn btn-primary" v-for="brand in brandList" :key="brand" :for="brand">
                  <input type="checkbox" :id="brand" :value="brand" v-model="selectedBrand"> {{ brand }}</label>                            
              </div> 
            </div>
          </div>
          <div class="form-group">
            <div class="col-lg-offset-1 col-lg-1">
              <button class="btn btn-primary"
                @click.prevent="deleteBrand">刪除
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
      brand: "",
      brandList: [],
      selectedBrand: []
    };
  },
  mounted() {
    this.getBrand();
  },
  computed: {},
  methods: {
    getBrand() {
      axios
        .get("/BrandList")
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) 
            this.brandList = ret;          
        })
        .catch(err => {
          alert(err);
        });
    },
    deleteBrand() {
      const param = this.selectedBrand.join(",");
      axios
        .delete(`/BrandList/${encodeURIComponent(param)}`)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功");
            this.getBrand();
          }
        })
        .catch(err => alert(err));
    }
  },
  components: {}
};
</script>
