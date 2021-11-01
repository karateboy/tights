<template>
  <div>
    <div
      class="modal inmodal"
      id="changeDyeCardModal"
      tabindex="-1"
      role="dialog"
      aria-hidden="true"
    >
      <div class="modal-dialog">
        <div class="modal-content animated fadeIn">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal">
              <span aria-hidden="true">&times;</span>
              <span class="sr-only">Close</span>
            </button>
            <h4 class="modal-title">更換漂染卡</h4>
          </div>
          <div class="modal-body">
            <form>
              <div class="form-group">
                <label class="col-lg-3 control-label">原漂染卡號:</label>
                <div class="col-lg-9">
                  <p>{{ workCard.dyeCardID }}</p>
                </div>
              </div>
              <div class="form-group">
                <label class="col-lg-3 control-label">新漂染卡號:</label>
                <div class="col-lg-9">
                  <div class="btn-group" data-toggle="buttons">
                    <label
                      class="btn btn-outline btn-primary dim"
                      v-for="dyeCard in targetDyeCardList"
                      :key="dyeCard._id"
                      @click="targetDyeCard = dyeCard"
                    >
                      <input type="radio" />
                      {{ dyeCard._id }}
                    </label>
                  </div>
                </div>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-white" data-dismiss="modal">
              取消
            </button>
            <button
              type="button"
              class="btn btn-primary"
              data-dismiss="modal"
              @click="applyChangeDyeCard"
            >
              確認
            </button>
          </div>
        </div>
      </div>
    </div>
    <div
      class="modal inmodal"
      id="updateWorkCardModal"
      tabindex="-1"
      role="dialog"
      aria-hidden="true"
    >
      <div class="modal-dialog">
        <div class="modal-content animated fadeIn">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal">
              <span aria-hidden="true">&times;</span>
              <span class="sr-only">Close</span>
            </button>
            <h4 class="modal-title">修改流動卡</h4>
          </div>
          <div class="modal-body">
            <form>
              <div class="form-group">
                <label class="col-lg-3 control-label">數量:</label>
                <input
                  type="text"
                  class="form-control"
                  v-model="workCard.newQuantity"
                />
              </div>
              <div class="form-group">
                <label class="col-lg-3 control-label">從庫存:</label>
                <input
                  type="text"
                  class="form-control"
                  v-model="workCard.newInventory"
                />
              </div>
            </form>
            <br />
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-white" data-dismiss="modal">
              取消
            </button>
            <button
              type="button"
              class="btn btn-primary"
              data-dismiss="modal"
              @click="updateWorkCard"
            >
              確認
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="workCardList.length != 0">
      <pagination
        for="workCardList"
        :records="total"
        :per-page="5"
        :options="paginationOption"
        v-model="current"
        @paginate="handlePageChange"
      ></pagination>
      <table class="table table-bordered table-condensed">
        <thead>
          <tr class="info">
            <th></th>
            <th>流動卡編號</th>
            <th>訂單編號</th>
            <th class="text-center">
              數量
              <br />優/預定(打)
            </th>
            <th class="text-center">從庫存</th>
            <th class="text-center">品項</th>
            <th class="text-center">顏色</th>
            <th class="text-center">尺寸</th>
            <th class="text-center">漂染卡號</th>
            <th class="text-center">排程時間</th>
            <th class="text-center">狀態</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(workCard, idx) in workCardList"
            :key="workCard._id"
            :class="{ success: idx == detail }"
          >
            <td>
              <button class="btn btn-info" @click="showDetail(idx)">
                <i class="fa fa-eye"></i>&nbsp;內容
              </button>
              <button
                class="btn btn-primary"
                @click="copyWorkCard(idx)"
                data-toggle="modal"
                data-target="#updateWorkCardModal"
              >
                <i class="fa fa-pencil"></i>&nbsp;修改
              </button>
              <button
                class="btn btn-primary"
                @click="changeDyeCard(idx)"
                data-toggle="modal"
                data-target="#changeDyeCardModal"
              >
                <i class="fa fa-exchange"></i>&nbsp;轉換漂染卡
              </button>
            </td>
            <td class="text-right">{{ workCard._id }}</td>
            <td class="text-right">{{ workCard.orderId }}</td>
            <td class="text-right">{{ displayGoodQuantity(workCard) }}</td>
            <td class="text-right">
              {{ displayQuantity(workCard.inventory) }}
            </td>
            <td class="text-right">{{ displayName(workCard) }}</td>
            <td class="text-right">{{ displayColor(workCard) }}</td>
            <td class="text-right">{{ displaySize(workCard) }}</td>
            <td class="text-right">{{ workCard.dyeCardID }}</td>
            <td class="text-right">{{ displayTime(workCard.startTime) }}</td>
            <td>
              <i
                class="fa fa-hourglass-half"
                style="color:red"
                aria-hidden="true"
                v-if="workCard.active"
                >處理中</i
              >
              <i
                class="fa fa-check"
                style="color:green"
                aria-hidden="true"
                v-else
                >結束</i
              >
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="alert alert-info" role="alert">沒有符合的流動卡</div>
    <hr />
    <div v-if="detail >= 0">
      <work-card-detail :workCard="workCard"></work-card-detail>
    </div>
  </div>
</template>
<style></style>
<script>
import moment from 'moment';
import axios from 'axios';
import cardHelper from '../cardHelper';
import WorkCardDetail from './WorkCardDetail.vue';
import * as dozenExpr from '../dozenExp';
import MyPagination from './MyPagination.vue'


export default {
  props: {
    url: {
      type: String,
      required: true,
    },
    param: {
      type: [Object, Array],
    },
  },
  data() {
    return {
      workCardList: [],
      skip: 0,
      limit: 5,
      total: 0,
      current: 1,
      detail: -1,
      workCard: {},
      targetDyeCardList: [],
      targetDyeCard: {},
      paginationOption: {
        template: MyPagination,
        texts: {
          count: '第{from}到第{to}筆/共{count}筆|{count} 筆|1筆',
        },
      },
    };
  },
  mounted: function() {
    this.fetchCard(this.skip, this.limit);
  },
  watch: {
    url: function() {
      this.fetchCard(this.skip, this.limit);
    },
    param: function() {
      this.fetchCard(this.skip, this.limit);
    },
  },

  methods: {
    changeDyeCard(idx) {
      this.workCard = this.workCardList[idx];
      const targetColor = this.workCard.order.details[this.workCard.detailIndex]
        .color;

      axios
        .post('/QueryDyeCard/0/100', { color: targetColor, active: true })
        .then(resp => {
          const ret = resp.data;
          this.targetDyeCardList.splice(0, this.targetDyeCardList.length);

          for (let dyeCard of ret) {
            this.targetDyeCardList.push(dyeCard);
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    copyWorkCard(idx) {
      let newQuantity = this.displayQuantity(this.workCardList[idx].quantity);
      let newInventory = this.displayQuantity(this.workCardList[idx].inventory);
      this.workCard = this.workCardList[idx];
      this.$set(this.workCard, 'newQuantity', newQuantity);
      this.$set(this.workCard, 'newInventory', newInventory);
    },
    updateWorkCard() {
      this.workCard.quantity = dozenExpr.fromDozenStr(
        this.workCard.newQuantity
      );
      this.workCard.inventory = dozenExpr.fromDozenStr(
        this.workCard.newInventory
      );
      this.workCard.good = this.workCard.quantity - this.workCard.inventory;
      axios
        .post('/WorkCard', this.workCard)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert('成功');
          } else alert('失敗:' + ret.msg);
        })
        .catch(err => {
          alert(err);
        });
    },
    applyChangeDyeCard() {
      let url = `/MoveWorkCard/${this.workCard._id}/${this.workCard.dyeCardID}/${this.targetDyeCard._id}`;
      axios
        .get(url)
        .then(resp => {
          const ret = resp.data;
          if (ret.Ok) {
            this.workCard.dyeCardID = this.targetDyeCard._id;
            alert('成功');
          } else alert('失敗');
        })
        .catch(err => {
          alert(err);
        });
    },
    processResp(resp) {
      const ret = resp.data;
      this.workCardList.splice(0, this.workCardList.length);
      for (let workCard of ret) {
        cardHelper.populateWorkCard(workCard);
        this.workCardList.push(workCard);
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

    displayTime(millis) {
      return moment(millis).format('LLL');
    },
    showDetail(idx) {
      this.workCard = this.workCardList[idx];
      this.detail = idx;
    },
    displayName(workCard) {
      if (workCard.order.details != null) {
        return workCard.order.name;
      } else return '查詢中';
    },
    displayColor(workCard) {
      if (workCard.order.details != null) {
        return workCard.order.details[workCard.detailIndex].color;
      } else return '查詢中';
    },
    displaySize(workCard) {
      if (workCard.order && workCard.order.details) {
        return workCard.order.details[workCard.detailIndex].size;
      } else return '查詢中';
    },
    displayGoodQuantity(workCard) {
      //workCard.good + "/" +workCard.quantity
      return (
        dozenExpr.toDozenStr(workCard.good) +
        '/' +
        dozenExpr.toDozenStr(workCard.quantity)
      );
    },
    displayQuantity(v) {
      return dozenExpr.toDozenStr(v);
    },
  },
  components: {
    WorkCardDetail,
  },
};
</script>
