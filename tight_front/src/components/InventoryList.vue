<template>
  <div>
    <div v-if="inventoryList.length != 0">
      <pagination
        for="inventoryList"
        :records="total"
        :per-page="15"
        :options="paginationOption"
        v-model="current"
        @paginate="handlePageChange"
      ></pagination>
      <h3>數量總計:{{ quantityTotal }}</h3>
      <table class="table table-bordered table-condensed">
        <thead>
          <tr>
            <td colspan="9">
              <button class="btn btn-info ml-3" @click="upsertAll">
                <i class="fa fa-check"></i>&nbsp;全部儲存
              </button>
            </td>
          </tr>
          <tr class="info">
            <th></th>
            <th class="text-center">工廠代號</th>
            <th class="text-center">客戶編號</th>
            <th class="text-center">顏色</th>
            <th class="text-center">尺寸</th>
            <th class="text-center">品牌</th>
            <th class="text-center">在庫數量</th>
            <th class="text-center">排定用量</th>
            <th class="text-center">流動卡</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(inventory, idx) in inventoryList"
            :key="idx"
            :class="{ success: idx == detail }"
          >
            <td>
              <button class="btn btn-danger" @click="del(inventory)">
                <i class="fa fa-check"></i>&nbsp;刪除
              </button>
            </td>
            <td class="text-right">{{ inventory.factoryID }}</td>
            <td class="text-right">
              <input type="text" v-model="inventory.customerID" />
            </td>
            <td class="text-right">{{ inventory.color }}</td>
            <td class="text-right">{{ inventory.size }}</td>
            <td class="text-right">
              <input type="text" v-model="inventory.brand" />
            </td>
            <td class="text-right">
              <input type="number" v-model="inventory.quantityStr" />
            </td>
            <td class="text-right">
              {{ displayLoan(inventory.loan) }}
              <!--
                      <button class='btn btn-info' @click="refreshLoan(inventory)"><i class="fa fa-check"></i>&nbsp;重新整理</button>
                       -->
            </td>
            <td>
              <div
                v-for="workCardID in inventory.workCardList"
                :key="workCardID"
              >
                {{ workCardID }}
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="alert alert-info" role="alert">沒有符合的流動卡</div>
    <hr />
  </div>
</template>
<style></style>
<script>
import axios from 'axios';
// import { Pagination, PaginationEvent } from 'vue-pagination-2';
import * as dozenExpr from '../dozenExp';
import MyPagination from './MyPagination.vue';

export default {
  props: {
    url: {
      type: String,
      required: true,
    },
    param: {
      type: [Object],
    },
  },
  data() {
    let quantityTotal = '';
    return {
      inventoryList: [],
      current: 1,
      skip: 0,
      limit: 15,
      total: 0,
      detail: -1,
      paginationOption: {
        template: MyPagination,
        texts: {
          count: '第{from}到第{to}筆/共{count}筆|{count} 筆|1筆',
        },
      },
      quantityTotal,
    };
  },
  mounted() {
    this.fetchCard(this.skip, this.limit);
    //PaginationEvent.$on('vue-pagination::inventoryList', this.handlePageChange);
  },
  watch: {
    url: function() {
      this.skip = 0;
      this.current = 1;
      this.fetchCard(this.skip, this.limit);
    },
    param: function() {
      this.skip = 0;
      this.current = 1;
      this.fetchCard(this.skip, this.limit);
    },
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
      this.getQuantityTotal();
    },
    getQuantityTotal() {
      let paramJson = encodeURIComponent(JSON.stringify(this.param));
      let request_url = `${this.url}/${paramJson}/total`;
      axios
        .get(request_url, this.param)
        .then(resp => {
          this.quantityTotal =  dozenExpr.toDozenStr(resp.data);
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
            alert('成功更新!');
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    upsertAll() {
      let allP = [];
      for (let inventory of this.inventoryList) {
        inventory.quantity = dozenExpr.fromDozenStr(inventory.quantityStr);
        allP.push(axios.post(`/Inventory`, inventory));
      }
      Promise.all(allP).then(() => {
        alert('成功更新!');
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
            alert('成功刪除!');
            this.fetchCard(this.skip, this.limit);
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    displayLoan(q) {
      if (!q) {
        return '-';
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
            alert('成功更新!');
          }
        })
        .catch(err => {
          alert(err);
        });
    },
  },
  components: {
    //    Pagination,
  },
};
</script>
