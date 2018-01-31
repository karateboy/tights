<template>
    <div>
        <div v-if="inventoryList.length != 0">
            <table class="table table-bordered table-condensed">
                <thead>
                <tr class='info'>
                    <th></th>
                    <th class='text-center'>工廠代號</th>
                    <th class='text-center'>客戶編號</th>
                    <th class='text-center'>顏色</th>
                    <th class='text-center'>尺寸</th>
                    <th class='text-center'>在庫數量</th>
                    <th class='text-center'>排定用量</th>
                    <th class='text-center'>流動卡</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for='(inventory, idx) in inventoryList' :class='{success:idx==detail}'>
                    <td><button class='btn btn-info' @click="upsert(inventory)"><i class="fa fa-check"></i>&nbsp;儲存</button>
                      <button class='btn btn-danger' @click="del(inventory)"><i class="fa fa-check"></i>&nbsp;刪除</button>
                    </td>
                    <td class='text-right'>{{inventory.factoryID}}</td>
                    <td class='text-right'><input type="text" v-model="inventory.customerID"></td>
                    <td class='text-right'>{{inventory.color}}</td>
                    <td class='text-right'>{{inventory.size}}</td>
                    <td class='text-right'>
                      <input type="number" v-model="inventory.quantityStr"> 
                    </td>
                    <td class='text-right'>{{displayLoan(inventory.loan)}}
                      <!--
                      <button class='btn btn-info' @click="refreshLoan(inventory)"><i class="fa fa-check"></i>&nbsp;重新整理</button>
                       -->                      
                    </td>
                    <td>
                      <div v-for='workCardID in inventory.workCardList'>{{workCardID}}</div>
                    </td>
                </tr>
                </tbody>
            </table>
            <pagination for="inventoryList" :records="total" :per-page="5"
                        count-text="第{from}到第{to}筆/共{count}筆|{count} 筆|1筆"></pagination>
        </div>
        <div v-else class="alert alert-info" role="alert">沒有符合的流動卡</div>
        <hr>
    </div>
</template>
<style>

</style>
<script>
import moment from "moment";
import axios from "axios";
import { Pagination, PaginationEvent } from "vue-pagination-2";
import * as dozenExpr from "../dozenExp";

export default {
  props: {
    url: {
      type: String,
      required: true
    },
    param: {
      type: [Object]
    }
  },
  data() {
    return {
      inventoryList: [],
      skip: 0,
      limit: 5,
      total: 0,
      detail: -1
    };
  },
  mounted() {
    this.fetchCard(this.skip, this.limit);
    PaginationEvent.$on("vue-pagination::inventoryList", this.handlePageChange);
  },
  watch: {
    url: function() {
      this.fetchCard(this.skip, this.limit);
    },
    param: function() {
      this.fetchCard(this.skip, this.limit);
    }
  },

  methods: {
    processResp(resp) {
      const ret = resp.data;
      this.inventoryList.splice(0, this.inventoryList.length);
      for (let inventory of ret) {
        inventory.quantityStr = dozenExpr.toDozenStr(inventory.quantity);
        this.inventoryList.push(inventory);
      }
    },
    fetchCard(skip, limit) {
      let paramJson = encodeURIComponent(JSON.stringify(this.param));

      let request_url = `${this.url}/${paramJson}/${skip}/${limit}`;

      axios
        .get(request_url)
        .then(this.processResp)
        .catch(err => {
          alert(err);
        });
      this.fetchCardCount();
    },
    fetchCardCount() {
      let paramJson = encodeURIComponent(JSON.stringify(this.param));
      let request_url = `${this.url}/${paramJson}/count`;
      axios
        .get(request_url, this.param)
        .then(resp => {
          this.total = resp.data;
        })
        .catch(err => {
          alert(err);
        });
    },
    handlePageChange(page) {
      this.skip = (page - 1) * this.limit;
      this.fetchCard(this.skip, this.limit);
    },
    upsert(inventory) {
      let url = `/Inventory`;
      inventory.quantity = dozenExpr.fromDozenStr(inventory.quantityStr);
      axios
        .post(url, inventory)
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            alert("成功更新!");
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    del(inventory) {
      let paramJson = encodeURIComponent(JSON.stringify(inventory));
      let url = `/Inventory/${paramJson}`;

      axios
        .delete(url)
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            alert("成功刪除!");
            this.fetchCard(this.skip, this.limit);
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    displayLoan(q) {
      if (!q) {
        return "-";
      } else {
        return dozenExpr.toDozenStr(q);
      }
    },
    refreshLoan(inventory) {
      let url = `/RefreshInventoryLoan`;
      axios
        .post(url, inventory)
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            alert("成功更新!");
          }
        })
        .catch(err => {
          alert(err);
        });
    }
  },
  components: {
    Pagination
  }
};
</script>
