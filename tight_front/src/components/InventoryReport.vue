<template>
  <div>
    <br />
    <div class="form-horizontal">
      <div class="form-group">
        <label class="col-lg-1 control-label">工廠代號:</label>
        <div class="col-lg-4">
          <input
            type="text"
            class="form-control"
            v-model="inventory.factoryID"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">客戶編號:</label>
        <div class="col-lg-4">
          <input
            type="text"
            class="form-control"
            v-model="inventory.customerID"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">品牌:</label>
        <div class="col-lg-4">
          <input type="text" class="form-control" v-model="inventory.brand" />
        </div>
        <div class="col-lg-7">
          <div class="btn-group" data-toggle="buttons">
            <label
              class="btn btn-outline btn-primary"
              v-for="brand in brandList"
              :key="brand"
              @click="inventory.brand = brand"
            >
              <input type="radio" />{{ brand }}
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <div class="col-lg-offset-1 col-lg-3">
          <button class="btn btn-primary" @click.prevent="query">
            查詢庫存
          </button>
          <button class="btn btn-primary mr-2" @click.prevent="downloadExcel">
            下載報表
          </button>
        </div>
      </div>
    </div>
    <spinner v-if="loading"></spinner>
    <inventory-list
      v-if="display"
      url="/QueryInventory"
      :param="queryParam"
    ></inventory-list>
  </div>
</template>
<style scoped>
body {
  background-color: #ff0000;
}
</style>
<script>
import * as dozenExp from '../dozenExp';
import Spinner from 'vue-simple-spinner';
import InventoryList from './InventoryList.vue';
import baseUrl from '../baseUrl';
import axios from 'axios'

export default {
  data() {
    return {
      inventory: {
        factoryID: undefined,
        customerID: undefined,
        color: undefined,
        size: undefined,
        brand: undefined,
        quantity: 0,
      },
      brandList: [],
      loading: false,
      display: false,
      queryParam: {},
    };
  },
  mounted() {
    this.getBrandList();
  },
  computed: {
    dozenNumber: {
      get: function() {
        return dozenExp.toDozenStr(this.inventory.quantity);
      },
      set: function(v) {
        this.inventory.quantity = dozenExp.fromDozenStr(v);
      },
    },
  },
  methods: {
    getBrandList() {
      axios
        .get('/BrandList')
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            this.brandList.splice(0, this.brandList.length);
            for (let brand of ret) {
              this.brandList.push(brand);
            }
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    query() {
      let param = {};
      if (this.inventory.factoryID) param.factoryID = this.inventory.factoryID;

      if (this.inventory.customerID)
        param.customerID = this.inventory.customerID;

      if (this.inventory.brand) param.brand = this.inventory.brand;

      if (!this.display) this.display = true;
      this.queryParam = JSON.parse(JSON.stringify(param));
    },
    downloadExcel() {
      let param = {};
      if (this.inventory.factoryID) param.factoryID = this.inventory.factoryID;

      if (this.inventory.customerID)
        param.customerID = this.inventory.customerID;

      if (this.inventory.brand) param.brand = this.inventory.brand;

      JSON.stringify(param);

      const url = `${baseUrl()}/QueryInventory/Excel/${JSON.stringify(param)}`;
      window.open(url);
    },
  },
  components: {
    InventoryList,
    Spinner,
  },
};
</script>
