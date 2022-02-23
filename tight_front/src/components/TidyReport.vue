<template>
  <div>
    <div class="form-horizontal">
      <div class="form-group">
        <label class="col-lg-1 control-label">訂單編號:</label>
        <div class="col-lg-4">
          <input
            type="text"
            placeholder="訂單編號"
            class="form-control"
            v-model="queryParam.orderID"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">顏色:</label>
        <div class="col-lg-4">
          <input
            type="text"
            placeholder="顏色"
            class="form-control"
            v-model="queryParam.color"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">尺寸:</label>
        <div class="col-lg-4">
          <input
            type="text"
            placeholder="尺寸"
            class="form-control"
            v-model="queryParam.size"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">日期從:</label>
        <div class="col-lg-5">
          <div class="input-daterange input-group">
            <span class="input-group-addon"
              ><i class="fa fa-calendar"></i
            ></span>
            <datepicker
              v-model="start"
              language="zh"
              format="yyyy-MM-dd"
            ></datepicker>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label class="col-lg-1 control-label">至(不含):</label>
        <div class="col-lg-5">
          <div class="input-daterange input-group">
            <span class="input-group-addon"
              ><i class="fa fa-calendar"></i
            ></span>
            <datepicker
              v-model="end"
              language="zh"
              format="yyyy-MM-dd"
            ></datepicker>
          </div>
        </div>
      </div>
      <div class="form-group">
        <div class="col-lg-1" />
        <div class="col-lg-2" v-for="phase in phaseList" :key="phase">
          <button class="btn btn-primary" @click="queryByPhase(phase)">
            查詢({{ phase }})
          </button>
        </div>
      </div>
    </div>
    <div v-if="showReport">
      <label
        class="btn btn-outline"
        data-toggle="tooltip"
        data-placement="bottom"
        title="Excel"
        ><a @click.prevent="downloadExcel"
          ><i class="fa fa-file-excel-o fa-2x"></i>下載Excel</a
        ></label
      >
      <h1>{{ reportTitle }}</h1>
      <table class="table  table-bordered table-condensed">
        <thead>
          <tr class="info">
            <th>輸入日期</th>
            <th>結束日期</th>
            <th>訂單編號</th>
            <th>流動卡編號</th>
            <th>客戶編碼</th>
            <th>工廠代碼</th>
            <th>顏色</th>
            <th>尺寸</th>
            <th>工作階段</th>
            <th>優</th>
            <th>副</th>
            <th>汙</th>
            <th>破</th>
            <th>副未包</th>
            <th>工號</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="card in cardList"
            :key="card._id.workCardID + card._id.phase"
          >
            <td>{{ displayDate(card.date) }}</td>
            <td>{{ displayDate(card.finishDate) }}</td>
            <td>{{ displayOrderID(card) }}</td>
            <td>{{ card._id.workCardID }}</td>
            <td>{{ displayCustomerID(card) }}</td>
            <td>{{ displayFactoryID(card) }}</td>
            <td>{{ displayColor(card) }}</td>
            <td>{{ displaySize(card) }}</td>
            <td>{{ card._id.phase }}</td>
            <td>{{ displayQuantity(card.good) }}</td>
            <td>{{ displayQuantity(card.sub) }}</td>
            <td>{{ displayQuantity(card.stain) }}</td>
            <td>{{ displayQuantity(card.broken) }}</td>
            <td>{{ displayQuantity(card.subNotPack) }}</td>
            <td>{{ card.operator }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <th>小計</th>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <th>{{ displayQuantity(good) }}</th>
            <th>{{ displayQuantity(sub) }}</th>
            <th>{{ displayQuantity(stain) }}</th>
            <th>{{ displayQuantity(broken) }}</th>
            <th>{{ displayQuantity(subNotPack) }}</th>
            <td></td>
          </tr>
        </tfoot>
      </table>
    </div>
  </div>
</template>
<style>
body {
  background-color: #ff0000;
}
</style>
<script>
import axios from 'axios';
import moment from 'moment';
import Datepicker from 'vuejs-datepicker';
import * as dozenExp from '../dozenExp';
import baseUrl from '../baseUrl';
import cardHelper from '../cardHelper';
const FileDownload = require('js-file-download');

export default {
  data() {
    return {
      queryParam: {
        orderID: '',
        color: '',
        size: '',
      },
      showReport: false,
      cardList: [],
      good: 0,
      sub: 0,
      stain: 0,
      broken: 0,
      subNotPack: 0,
      phaseList: ['檢襪', '車洗標', '剪線頭', '整理包裝'],
      reportTitle: '',
      downloadUrl: '',
    };
  },
  computed: {
    start: {
      get: function() {
        if (this.queryParam.start)
          return moment(this.queryParam.start).toDate();
        else {
          const start = moment('0', 'hh').toDate();
          this.queryParam.start = start.getTime();
          return start;
        }
      },
      // setter
      set: function(newValue) {
        this.queryParam.start = newValue.getTime();
      },
    },
    end: {
      get: function() {
        if (this.queryParam.end) return moment(this.queryParam.end).toDate();
        else {
          const end = moment('0', 'hh')
            .add(1, 'day')
            .toDate();
          this.queryParam.end = end.getTime();
          return end;
        }
      },
      // setter
      set: function(newValue) {
        this.queryParam.end = newValue.getTime();
      },
    },
  },
  methods: {
    query() {
      const url =
        '/TidyReport/' + this.queryParam.start + '/' + this.queryParam.end;
      axios
        .get(url)
        .then(resp => {
          const ret = resp.data;
          this.cardList.splice(0, this.cardList.length);
          for (let card of ret) {
            cardHelper.populateTidyCard(card);
            this.cardList.push(card);
          }
          this.showReport = true;
        })
        .catch(err => {
          alert(err);
        });
    },
    queryByPhase(phase) {
      this.reportTitle = `${phase}報表`;
      this.downloadUrl =
        baseUrl() +
        `/TidyReportByPhase/Excel/${phase}/${this.queryParam.start}/${this.queryParam.end}`;

      this.queryParam.phase = phase;
      const url = `/TidyReportByPhase`;
      axios
        .get(url, { params: this.queryParam })
        .then(resp => {
          const ret = resp.data;
          this.cardList = [];
          this.good = 0;
          this.sub = 0;
          this.stain = 0;
          this.broken = 0;
          this.subNotPack = 0;
          for (let card of ret) {
            this.cardList.push(card);
            this.good += card.good;
            this.sub += card.sub;
            this.stain += card.stain;
            this.broken += card.broken;
            this.subNotPack += card.subNotPack;
          }
          this.showReport = true;
        })
        .catch(err => {
          alert(err);
        });
    },
    displayOrderID(tidyCard) {
      if (tidyCard.workCard.order) return tidyCard.workCard.order._id;
      else return '查詢中';
    },
    displayCustomerID(tidyCard) {
      if (tidyCard.workCard.order) return tidyCard.workCard.order.customerId;
      else return '查詢中';
    },
    displayFactoryID(tidyCard) {
      if (tidyCard.workCard.order) return tidyCard.workCard.order.factoryId;
      else return '查詢中';
    },
    displayColor(tidyCard) {
      if (tidyCard.workCard.order && tidyCard.workCard.order.details) {
        return tidyCard.workCard.order.details[tidyCard.workCard.detailIndex]
          .color;
      } else return '查詢中';
    },
    displaySize(tidyCard) {
      if (tidyCard.workCard.order && tidyCard.workCard.order.details) {
        return tidyCard.workCard.order.details[tidyCard.workCard.detailIndex]
          .size;
      } else return '查詢中';
    },
    displayDate(mm) {
      if (mm) return moment(mm).format('YYYY-MM-DD');
      else return '';
    },
    displayQuantity(v) {
      return dozenExp.toDozenStr(v);
    },
    downloadExcel() {
      axios
        .get('/TidyReportByPhase/Excel', {
          params: this.queryParam,
          responseType: 'blob',
        })
        .then(resp => {
          const phase = this.queryParam.phase;
          FileDownload(resp.data, `整理報表(${phase}).xlsx`);
        })
        .catch(err => {
          alert(err);
        });
    },
  },
  components: {
    Datepicker,
  },
};
</script>
