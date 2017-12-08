<template>
    <div>
        <div v-if="inventoryList.length != 0">
            <table class="table table-bordered table-condensed">
                <thead>
                <tr class='info'>
                    <th>工廠代號</th>
                    <th class='text-center'>顏色</th>
                    <th class='text-center'>尺寸</th>
                    <th class='text-center'>數量(隻)</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for='(inventory, idx) in inventoryList' :class='{success:idx==detail}'>
                    <td class='text-right'>{{inventory.factoryID}}</td>
                    <td class='text-right'>{{inventory.color}}</td>
                    <td class='text-right'>{{inventory.size}}</td>
                    <td class='text-right'>
                      <input type="number" v-model.number="inventory.quantity"> 
                      <button class='btn btn-info' @click="upsert(inventory)"><i class="fa fa-check"></i>&nbsp;儲存</button>
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
      type: [Object, Array]
    }
  },
  data() {
    return {
      inventoryList: [],
      skip: 0,
      limit: 10,
      total: 0,
      detail: -1,
      inventory: {}
    };
  },
  mounted: function() {
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
        this.inventoryList.push(inventory);
      }
    },
    fetchCard(skip, limit) {
      let request_url = `${this.url}/${skip}/${limit}`;

      if (this.param) {
        axios
          .post(request_url, this.param)
          .then(this.processResp)
          .catch(err => {
            alert(err);
          });
      } else {
        axios
          .get(request_url)
          .then(this.processResp)
          .catch(err => {
            alert(err);
          });
      }
      this.fetchCardCount();
    },
    fetchCardCount() {
      let request_url = `${this.url}/count`;
      if (this.param) {
        axios
          .post(request_url, this.param)
          .then(resp => {
            this.total = resp.data;
          })
          .catch(err => {
            alert(err);
          });
      } else {
        axios
          .get(request_url)
          .then(resp => {
            this.total = resp.data;
          })
          .catch(err => {
            alert(err);
          });
      }
    },
    handlePageChange(page) {
      this.skip = (page - 1) * this.limit;
      this.fetchCard(this.skip, this.limit);
    },
    upsert(inventory) {
      let url = `/UpsertInventory`;

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
