package cn.xingbao.config;

import cn.xingbao.controller.SystemSettingController;
import cn.xingbao.domain.*;
import cn.xingbao.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {
  @Bean CommandLineRunner seed(ToolRepository tools, RiskKeywordRuleRepository keywords, GrowthRecordTypeRepository recordTypes, SystemSettingRepository settings, LearningContentRepository learning, CognitionContentRepository cognition) {
    return args -> { seedTools(tools); if (cognition.count() == 0) seedCognition(cognition); if (learning.count() == 0) seedLearning(learning); if (keywords.count() == 0) seedKeywords(keywords); if (recordTypes.count() == 0) seedRecordTypes(recordTypes); if (settings.findBySettingKeyAndDeletedFalse(SystemSettingController.COMMUNITY_MAX_IMAGES).isEmpty()) { SystemSetting setting = new SystemSetting(); setting.setSettingKey(SystemSettingController.COMMUNITY_MAX_IMAGES); setting.setSettingValue("2"); setting.setRemark("社区发帖最大图片数量"); settings.save(setting); } };
  }
  private static void seedTools(ToolRepository repo) {
    if (repo.count() == 0) for (String[] item : new String[][] {{"SCALE", "筛查量表使用说明", "帮助家长理解家庭观察记录的用途与边界。", "平台编辑"}, {"POLICY", "特殊儿童帮扶政策查询", "按地区、事项查询可进一步了解的公共服务与帮扶信息。", "公开政策"}, {"RESOURCE", "视觉提示卡使用指南", "提供居家教学与行为支持中视觉提示卡的基础用法。", "平台编辑"}, {"MANUAL", "情绪应急手册", "在高压力情绪场景中，帮助家人先稳住现场与关系。", "平台编辑"}}) { ToolResource tool = new ToolResource(); tool.setType(item[0]); tool.setTitle(item[1]); tool.setContent(item[2]); tool.setSource(item[3]); repo.save(tool); }
    if (repo.findByTypeAndDeletedFalse("COGNITION").isEmpty()) { ToolResource tool = new ToolResource(); tool.setType("COGNITION"); tool.setTitle("家长认知小课堂"); tool.setContent("澄清常见误解，学习更理解、更支持的陪伴方式。"); tool.setSource("平台编辑"); repo.save(tool); }
  }
  private static void seedCognition(CognitionContentRepository repo) {
    addCognition(repo,"爸妈先把情绪整理好，真的很重要吗？","先照顾好自己，才更有余力陪伴孩子。","重要，但不是要求爸爸妈妈永远不崩溃。大人紧绷时，孩子也容易感到压力。可以先停十秒、喝口水、把声音放低；需要时请家人接手几分钟。对自己说一句：今天很难，但我已经在努力了。",10);
    addCognition(repo,"“孤独症”和“自闭症”有什么区别？","日常中文里常常是在说同一类情况。","更正式的说法常见为“孤独症谱系障碍 / 自闭症谱系障碍”。“谱系”意思是每个孩子表现和需要的支持不一样，不宜用一个模板判断所有孩子。",20);
    addCognition(repo,"孤独症 / 自闭症是怎么来的？是不是谁做错了？","不是父母教养造成，也不要归咎于某一个人。","不能把它归到单一原因上。研究认为与遗传因素和早期发展中的多种因素有关；不是父母教养造成，也不是孩子打疫苗造成。把精力放在理解孩子、寻求合适支持上，比反复追责更有用。",30);
    addCognition(repo,"为什么有些星宝不喜欢嘈杂声音？","别人觉得还好的声音，对他可能很难受。","有的孩子对声音、灯光、触感或人多的环境更敏感。可先降一点音量、提前预告、允许戴耳罩或到安静处休息；观察什么最容易让他不舒服。",40);
    addCognition(repo,"为什么叫他，他有时像没听见？","不一定是不礼貌或故意不理人。","孩子可能正专注、处理信息需要时间，或当下环境太吵。试着靠近一些，叫一次名字后用短句说重点，再等 5–10 秒；少一些连珠炮式催促。",50);
    addCognition(repo,"为什么会有一些小动作、来回走或重复玩？","安全时，不必急着制止。","有些重复小动作可能帮助孩子调节紧张、开心或等待时的感受。先想一想：他是不是累了、吵了、等太久了，还是在让自己平静？",60);
    addCognition(repo,"为什么总在特定时间、地点坚持固定做法？","这常被称为“刻板”或坚持固定性。","例如必须走同一条路、杯子要放原来的位置、先做哪件事不能变。固定流程更好预测，能减少不安；有时也是孩子让自己稳下来的办法，不等于故意犟。确实要改变时，提前预告，用图片或简单顺序卡说明，并从很小的变化开始。",70);
    addCognition(repo,"为什么有时会斜着眼睛看东西？","既可能是视觉偏好，也应留意视力与眼位。","有些孩子会从侧面看、凑近看，或特别爱看转动、发亮的东西；单凭这一种表现不能判断原因。若经常持续斜眼、眯眼、歪头看、两眼看起来不对齐、总凑很近或频繁揉眼，建议尽早做眼科或眼视光检查。家里不用强行掰正头部，可记录出现的时间和场景供就诊参考。",80);
    addCognition(repo,"为什么有时会突然大吵大闹？","先把它看成孩子正在很难受的信号。","环境太吵、太挤、计划突然改变、等得太久、身体不舒服、听不懂，或想表达却说不出来，都可能让压力攒满后爆出来。当下先保证安全，少说话、放低声音、减少刺激并给一点空间；平静后再复盘原因。若频繁发生、有自伤伤人风险或怀疑疼痛生病，请及时咨询专业人员。",90);
    addCognition(repo,"爷爷奶奶可以怎么支持星宝和爸妈？","多一点理解，就是很大的支持。","少说“别惯着”“怎么还不会”，多问“现在需要我帮什么”。可以帮忙准备安静角、一起按固定流程做事、给爸妈十分钟喘口气。看到孩子的小进步时，说具体一点：刚才你等到了，真不容易。",100);
  }
  private static void addCognition(CognitionContentRepository repo,String title,String summary,String content,int sort){CognitionContent item=new CognitionContent();item.setTitle(title);item.setSummary(summary);item.setContent(content);item.setSortOrder(sort);repo.save(item);}
  private static void seedLearning(LearningContentRepository repo){addLearning(repo,"沟通表达","💬","用“二选一”开启表达",10);addLearning(repo,"日常自理","🧦","把大任务拆成小步骤",20);addLearning(repo,"情绪陪伴","🌤️","情绪来时，先连接再引导",30);addLearning(repo,"家庭记录","📝","每天只记三件小事",40);addLearning(repo,"外出准备","🎒","外出前的预告与选择",50);addLearning(repo,"通用方法","➡️","试试“先……再……”",60);addLearning(repo,"家长支持","☕","给自己三分钟缓冲",70);}
  private static void seedKeywords(RiskKeywordRuleRepository repo){add(repo,"根治","HIGH",true,10,"医疗夸大宣传");add(repo,"治愈","HIGH",true,10,"医疗夸大宣传");add(repo,"百分百康复","HIGH",true,10,"医疗夸大宣传");add(repo,"微信","HIGH",true,20,"站外导流");add(repo,"付款","HIGH",true,20,"交易付款");add(repo,"转账","HIGH",true,20,"交易付款");add(repo,"高价","MEDIUM",true,30,"疑似二手售卖");add(repo,"私下交易","MEDIUM",true,30,"交易导流");}
  private static void seedRecordTypes(GrowthRecordTypeRepository repo){addType(repo,"POSITIVE_BEHAVIOR","正向行为",10,"记录积极表现与进步");addType(repo,"PROBLEM_BEHAVIOR","问题行为",20,"记录需要关注的行为");addType(repo,"INTERVENTION","干预训练",30,"记录居家或专业干预训练");}
  private static void add(RiskKeywordRuleRepository repo,String keyword,String level,boolean review,int priority,String remark){RiskKeywordRule item=new RiskKeywordRule();item.setKeyword(keyword);item.setRiskLevel(level);item.setReviewRequired(review);item.setPriority(priority);item.setRemark(remark);repo.save(item);}
  private static void addType(GrowthRecordTypeRepository repo,String code,String name,int sort,String remark){GrowthRecordType item=new GrowthRecordType();item.setCode(code);item.setName(name);item.setSortOrder(sort);item.setRemark(remark);repo.save(item);}
  private static void addLearning(LearningContentRepository repo,String category,String icon,String title,int sort){LearningContent item=new LearningContent();item.setCategory(category);item.setIcon(icon);item.setTitle(title);item.setIntro("一份可快速上手的家庭方法参考。");item.setSteps("选择一个容易开始的情境\n用简短清晰的方式提示\n等待回应并给予具体肯定\n记录有效的做法");item.setTip("内容仅供家庭日常参考，不替代专业建议。");item.setSortOrder(sort);repo.save(item);}
}
