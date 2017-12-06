<template>
    <div>
        <br>
        <div class="form-horizontal">
          <div class="form-group"><label class="col-lg-1 control-label">工廠代號:</label>
            <div class="col-lg-4"><input type="text" class="form-control" v-model="inventory.factoryID"></div>
          </div>
          <div class="form-group"><label class="col-lg-1 control-label">顏色:</label>
            <div class="col-lg-4"><input type="text" class="form-control" v-model="inventory.color"></div>
          </div>
          <div class="form-group">
            <label class="col-lg-1 control-label">尺寸:</label>
            <div class="col-lg-1"><input type="text" class="form-control" v-model="inventory.size"></div>
            <div class="col-lg-10">
              <div class="btn-group" data-toggle="buttons">
                <label class="btn btn-outline btn-primary" v-for="sizeOpt in sizeList" @click="inventory.size=sizeOpt">
                <input type="radio">{{ sizeOpt }} </label>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label class="col-lg-1 control-label">數量(打):</label>
              <div class="col-lg-2"><input type="text" class="form-control" v-model="dozenNumber"></div>
          </div>
          <div class="form-group">
            <div class="col-lg-1">
              <button class="btn btn-primary" :class="{disabled: !readyForQuery}"
                @click.prevent="query" :disabled="!readyForQuery">查詢庫存
              </button>
            </div>
            <div class="col-lg-offset-1 col-lg-1">
                <button class="btn btn-primary" :class="{disabled: !readyForQuery}"
                  @click.prevent="update" :disabled="!readyForQuery">更新庫存
                </button>
            </div>
          </div>
        </div>
        <spinner v-if="loading"></spinner>
    </div>
</template>
<style scoped>
body {
  background-color: #ff0000;
}
</style>
<script>
import axios from "axios";
import * as dozenExp from "../dozenExp";
import Spinner from 'vue-simple-spinner'

export default {
  data() {
    return {
      inventory: {
        factoryID: "",
        color: "",
        size: "",
        quantity: 0
      },
      loading:false,
      sizeList: [
        "XS/S",
        "S/M",
        "M/L",
        "L/XL",
        "2-6",
        "8-12",
        "XXS",
        "XS",
        "SS",
        "S",
        "M",
        "L",
        "XL",
        "XXL",
        "M/S",
        "T",
        "A/B",
        "C/D",
        "A",
        "B",
        "C",
        "D",
        "E",
        "XSml",
        "Sml",
        "Med",
        "Lge",
        "Xlge",
        "Sml/Med",
        "Lge/Xlge",
        "ChSml",
        "ChLge",
        "Adult",
        "ChSml/ChMed",
        "ChMed/ChLge",
        "ChLge/ChXLge",
        "0-6",
        "6-12",
        "12-24",
        "2T3T",
        "4T5T",
        "6-8"
      ]
    };
  },
  computed: {
    dozenNumber: {
      get: function() {
        return dozenExp.toDozenStr(this.inventory.quantity);
      },
      set: function(v) {
        this.inventory.quantity = dozenExp.fromDozenStr(v);
      }
    },
    readyForQuery(){
      if(!this.inventory.factoryID || !this.inventory.size || !this.inventory.color)
        return false
      else
        return true
    }
  },
  methods: {
    query() {
      let url = `/QueryInventory`;
      this.loading = true
      axios
        .post(url, this.inventory)
        .then(resp => {
          const ret = resp.data
          if (resp.status == 200) {
            const inventoryList = ret
            if(inventoryList.length != 0)
              this.inventory.quantity = inventoryList[0].quantity
            else
              this.inventory.quantity = 0
          }
          this.loading = false
        })
        .catch(err => {
          this.loading = false
          alert(err);
        });
    },
    update() {
      let url = `/UpdateInventory`;

      axios
        .post(url, this.inventory)
        .then(resp => {
          const ret = resp.data
          if (resp.status == 200) {
            alert("成功更新!")
          }
        })
        .catch(err => {
          alert(err);
        });
    }

  },
  components: {
    Spinner
  }
};
</script>
