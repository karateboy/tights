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
        <label class="col-lg-1 control-label">顏色:</label>
        <div class="col-lg-1">
          <input type="text" class="form-control" v-model="inventory.color" />
        </div>
        <div class="col-lg-10">
          <div class="btn-group" data-toggle="buttons">
            <label
              class="btn btn-outline btn-primary"
              v-for="color in colorList"
              :key="color"
              @click="inventory.color = color"
            >
              <input type="radio" />{{ color }}
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">尺寸:</label>
        <div class="col-lg-1">
          <input type="text" class="form-control" v-model="inventory.size" />
        </div>
        <div class="col-lg-10">
          <div class="btn-group" data-toggle="buttons">
            <label
              class="btn btn-outline btn-primary"
              v-for="sizeOpt in sizeList"
              :key="sizeOpt"
              @click="inventory.size = sizeOpt"
            >
              <input type="radio" />{{ sizeOpt }}
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">品牌:</label>
        <div class="col-lg-2">
          <input type="text" class="form-control" v-model="inventory.brand" />
        </div>
        <div class="col-lg-9">
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
        <label class="col-lg-1 control-label">數量(打):</label>
        <div class="col-lg-2">
          <input type="text" class="form-control" v-model="dozenNumber" />
        </div>
      </div>
      <div class="form-group">
        <div class="col-lg-offset-1 col-lg-1">
          <button class="btn btn-primary" @click.prevent="query">
            查詢庫存
          </button>
        </div>
        <div class="col-lg-offset-1 col-lg-1">
          <button
            class="btn btn-primary"
            :class="{ disabled: !readyForUpsert }"
            @click.prevent="upsert"
            :disabled="!readyForUpsert"
          >
            新增庫存
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
import axios from 'axios';
import * as dozenExp from '../dozenExp';
import Spinner from 'vue-simple-spinner';
import InventoryList from './InventoryList.vue';

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
      loading: false,
      display: false,
      queryParam: {},
      colorList: [],
      brandList:[],
      sizeList: [
        'XS/S',
        'S/M',
        'M/L',
        'L/XL',
        '2-6',
        '8-12',
        'XXS',
        'XS',
        'SS',
        'S',
        'M',
        'L',
        'XL',
        'XXL',
        'M/S',
        'T',
        'A/B',
        'C/D',
        'A',
        'B',
        'C',
        'D',
        'E',
        'XSml',
        'Sml',
        'Med',
        'Lge',
        'Xlge',
        'Sml/Med',
        'Lge/Xlge',
        'ChSml',
        'ChLge',
        'Adult',
        'ChSml/ChMed',
        'ChMed/ChLge',
        'ChLge/ChXLge',
        '0-6',
        '6-12',
        '12-24',
        '2T3T',
        '4T5T',
        '6-8',
      ],
    };
  },
  mounted() {
    axios
      .get('/ColorSeq')
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
    readyForUpsert() {
      if (
        !this.inventory.factoryID ||
        !this.inventory.size ||
        !this.inventory.color
      )
        return false;
      else return true;
    },
  },
  methods: {
    getBrandList(){
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

      if (this.inventory.size) param.size = this.inventory.size;

      if (this.inventory.color) param.color = this.inventory.color;

      if (this.inventory.customerID)
        param.customerID = this.inventory.customerID;

      if(this.inventory.brand)  
        param.brand = this.inventory.brand;

      if (!this.display) this.display = true;
      this.queryParam = JSON.parse(JSON.stringify(param));
    },
    upsert() {
      let url = `/Inventory`;
      axios
        .post(url, this.inventory)
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            alert('成功新增!');
          }
        })
        .catch(err => {
          alert(err);
        });
    },
  },
  components: {
    InventoryList,
    Spinner,
  },
};
</script>
