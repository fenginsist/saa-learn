package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * SupervisorAgent 支持通过 instruction 使用占位符来读取前序Agent的输出，这在 SupervisorAgent 作为 SequentialAgent 的子Agent时特别有用：
 *
 *
 * 关键特性
 * 多步骤循环路由：子Agent执行完成后会返回监督者，监督者可以继续路由到其他Agent，实现多步骤任务处理
 * 智能决策：使用LLM分析当前状态和任务需求，动态选择最合适的子Agent
 * Instruction占位符支持：instruction 支持使用占位符（如 {article_content}）读取前序Agent的输出
 * 自定义系统提示：通过 systemPrompt 提供详细的决策规则和上下文
 * 自动重试机制：内置重试机制（最多2次），确保路由决策的可靠性
 * 任务完成控制：监督者可以返回 FINISH 来结束任务流程
 */
public class ch10_SupervisorInstruction {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 第一个Agent：写文章
        ReactAgent articleWriterAgent = ReactAgent.builder()
                .name("article_writer")
                .model(chatModel)
                .description("专业写作Agent，负责创作文章")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}。")
                .outputKey("article_content")
                .build();

        // 监督者的子Agent
        ReactAgent translatorAgent = ReactAgent.builder()
                .name("translator_agent")
                .model(chatModel)
                .description("擅长将文章翻译成各种语言")
                .instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。待翻译文章：{article_content}。")
                .outputKey("translator_output")
                .build();

        // 评审文章agent
        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .description("擅长对文章进行评审和修改")
                .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。待评审文章：{article_content}。")
                .outputKey("reviewer_output")
                .build();



        // 监督者的instruction使用占位符读取前序Agent的输出
        final String SUPERVISOR_INSTRUCTION = """
你是一个智能的内容处理监督者，你可以看到前序Agent的聊天历史与任务处理记录。当前，你收到了以下文章内容：

{article_content} 

请根据文章内容的特点，决定是进行翻译还是评审：
- 如果文章是中文且需要翻译，选择 translator_agent
- 如果文章需要评审和改进，选择 reviewer_agent
- 如果任务完成，返回 FINISH
""";

        final String SUPERVISOR_SYSTEM_PROMPT = """
你是一个智能的内容处理监督者，负责协调翻译和评审任务。

## 可用的子Agent及其职责

### translator_agent
- **功能**: 擅长将文章翻译成各种语言
- **输出**: translator_output

### reviewer_agent
- **功能**: 擅长对文章进行评审和修改
- **输出**: reviewer_output

## 响应格式
只返回Agent名称（translator_agent、reviewer_agent）或FINISH，不要包含其他解释。
""";

        final String SUPERVISOR_MAIN_AGENT_SYSTEM_PROMPT = """
你是一个智能的内容处理监督者，负责协调翻译和评审任务。

## 可用的子Agent及其职责

### translator_agent
功能：擅长将文章翻译成各种语言
输出：translator_output

### reviewer_agent
功能：擅长对文章进行评审和修改
输出：reviewer_output

## 响应格式

必须只返回合法的 JSON 数组。

需要翻译：
["translator_agent"]

需要评审：
["reviewer_agent"]

任务完成：
[]

不要输出任何解释、Markdown 或其他文字。
""";

        // ============================================================
        // Supervisor 的 mainAgent
        // ============================================================
        ReactAgent supervisorMainAgent = ReactAgent.builder()
                .name("supervisor_main_agent")
                .model(chatModel)
                .description("负责判断下一步应该调用翻译Agent还是评审Agent")
                .systemPrompt(SUPERVISOR_MAIN_AGENT_SYSTEM_PROMPT)
                .outputKey("supervisor_decision")
                .build();



        // ============================================================
        // Supervisor
        // ============================================================
        // 创建SupervisorAgent，instruction中包含占位符
        SupervisorAgent supervisorAgent = SupervisorAgent.builder()
                .name("content_supervisor")
                .description("内容处理监督者，根据前序Agent的输出决定翻译或评审")
                .model(chatModel)
                .systemPrompt(SUPERVISOR_SYSTEM_PROMPT)
                .instruction(SUPERVISOR_INSTRUCTION)
                .mainAgent(supervisorMainAgent)
                .subAgents(List.of(translatorAgent, reviewerAgent))
                .build();

        // ============================================================
        // SequentialAgent
        // ============================================================
        // 创建SequentialAgent，SupervisorAgent作为子Agent
        SequentialAgent sequentialAgent = SequentialAgent.builder()
                .name("content_processing_workflow")
                .description("内容处理工作流：先写文章，然后根据文章内容决定翻译或评审")
                .subAgents(List.of(articleWriterAgent, supervisorAgent))
                .build();

        // 使用
        Optional<OverAllState> result = sequentialAgent.invoke("帮我写一篇关于春天的短文，然后翻译成英文");

        System.out.println("result: " + result.map(OverAllState::toString).orElse(""));
        /**
         *
         *apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * 17:46:05.749 [main] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Calculated core pool size: 32 (CPU cores: 16)
         * 17:46:05.751 [main] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Calculated maximum pool size: 64 (CPU cores: 16)
         * 17:46:05.752 [main] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Calculated queue capacity: 1000
         * 17:46:21.658 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- Invoking mainAgent 'supervisor_main_agent' compiled graph with threadId: Optional[subgraph_supervisor_main_agent]
         * 17:46:22.066 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- MainAgentNodeAction: supervisor_next from last AssistantMessage = [reviewer_agent]
         * 17:46:22.085 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.SupervisorNodeFromState -- SupervisorNodeFromState: routingKey='supervisor_next', value='[reviewer_agent]', parsed agentNames=[reviewer_agent], validAgentNames=[reviewer_agent]
         * 17:46:55.206 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- Invoking mainAgent 'supervisor_main_agent' compiled graph with threadId: Optional[subgraph_supervisor_main_agent]
         * 17:46:55.602 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- MainAgentNodeAction: supervisor_next = FINISH from last AssistantMessage
         * 17:46:55.603 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentToSupervisorEdgeAction -- MainAgentToSupervisorEdgeAction: routing to END as value for key 'supervisor_next' is finish or empty: [FINISH]
         * result: {"OverAllState":{"data":{"_graph_execution_id_":"ed72534b-f607-4276-8ae1-ff36322b3992","input":"帮我写一篇关于春天的短文，然后翻译成英文","supervisor_next":["FINISH"],"subgraph_content_supervisor__compiled_graph":"[]","article_content":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"c6222819-ea48-9c0f-8cbd-d21a5d1b43cf","reasoningContent":""},"toolCalls":[],"media":[],"text":"当然可以。以下是我为您创作的一篇兼具诗意与温度的春天短文，文字凝练而富有画面感，力求在细微处见生机，在静默中听春声——\n\n**《春信》**  \n冬的余韵尚未散尽，风已悄然换了脾性：不再刺骨，只轻轻拂过耳际，像一句欲言又止的问候。柳枝最先接收到这密语，枯褐的枝条上，忽然鼓出点点青绒，仿佛大地在睡梦中微微睁开了眼。  \n\n桃李不争先后，各自酝酿着粉白的宣言；荠菜花则悄悄漫过田埂，在微光里铺开一片清亮的碎银。泥土松软温润，蚯蚓在暗处翻身，新翻的菜畦边，几粒豌豆正顶开薄土，怯生生地探出两片嫩绿的小手。  \n\n最是那清晨——露珠悬在蛛网上，晶莹得能照见整个微缩的春天；麻雀在刚抽芽的香椿树间扑棱棱飞起，抖落一串细碎的光。人站在院中，忽觉衣袖轻了，呼吸深了，心也跟着舒展如初展的叶脉。原来春天从不喧哗登场，它只是 quietly，把世界重新翻译成柔软、希望与可生长的模样。\n\n—— 作家手记：春非季节之名，乃万物重启的语法。\n\n---\n\n**English Translation: “The First Message of Spring”**  \nWinter’s echo has barely faded when the wind, too, changes its temper—no longer biting, but brushing softly past the ear like an unspoken greeting. The willow branches are the first to receive this quiet dispatch: tiny buds of downy green swell along their once-bare twigs, as if the earth itself were gently opening its eyes in sleep.  \n\nPeach and plum trees make no haste, each quietly preparing its rosy-white proclamation; meanwhile, shepherd’s-purse flowers slip unassumingly over field edges, spilling a shimmering scatter of silver light. The soil grows soft and warm. Beneath its surface, earthworms turn in the dark; beside freshly turned vegetable beds, pea seeds push through the thin crust of earth, shyly unfurling two tender green hands.  \n\nEspecially at dawn—the dewdrops hang suspended on spiderwebs, so clear they hold miniature springs within their curves; sparrows burst from newly leafing Chinese toon trees, scattering shards of light with every flutter of their wings. Standing in the courtyard, one suddenly feels the sleeves lighter, the breath deeper, the heart unfolding like a newly emerged leaf vein. For spring never arrives with fanfare—it simply arrives *quietly*, and rewrites the world into softness, hope, and the quiet certainty of growth.  \n\n— *Author’s Note: Spring is not merely a season—it is grammar reborn, the syntax by which all life begins again.*  \n\n如您有特定风格偏好（如更古典、童趣、哲思或地域特色），我很乐意为您重写或延展。愿这个春天，既有文字的微光，也有您窗外真实的风与花。"},"reviewer_output":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"08062b47-315a-9649-a3cc-ce3f9c0a2a47","reasoningContent":""},"toolCalls":[],"media":[],"text":"【评论家审读意见】  \n——评《春信》及其英译：一首以通感为笔、以节制为韵的微型春之赋格  \n\n作为一篇不足六百字的短章，《春信》远不止是应景小品，而是一次高度自觉的文学实践：它用汉语的肌理呼吸春天，又以翻译的镜面反照汉语的不可译性与可再生性。以下从立意、语言、结构与跨语转化四个维度作专业评述，并附精修建议（非“修改”，而是提供可供作者抉择的另一种可能）：\n\n---\n\n**一、立意：在“微物”中锚定存在诗学**  \n文章摒弃宏大抒情，将春天还原为可触、可听、可辨温的微观现场——“蚯蚓翻身”“豌豆探手”“蛛网悬露”，皆非修辞点缀，而是现象学式的凝视。尤为可贵的是结尾升维：“春非季节之名，乃万物重启的语法”，将自然律动提升至存在论层面，暗合海德格尔“语言是存在之家”的哲思，却全无术语堆砌，举重若轻。此为当代汉语散文稀缺的智性厚度。\n\n✅ 亮点：以“语法”喻春，既承袭古典“生生之谓易”的宇宙观，又赋予其现代语言哲学的锐度。\n\n⚠️ 建议微调：作家手记中“语法”一词稍显抽象。若追求更富张力的收束，可试作——  \n> *“春非四时之一，乃世界向自身发出的第一声元音。”*  \n（理由：“元音”兼具语言学本源性、发声的初生感与汉语单音节的韵律美，且与前文“欲言又止的问候”“密语”“宣言”形成声韵闭环）\n\n---\n\n**二、语言：汉语的“留白”与“触觉”双重胜利**  \n作者深谙汉语的未完成性美学：“怯生生地探出两片嫩绿的小手”——“怯生生”是拟人，“小手”是通感，“探出”是动态留白，三重叠加，使植物获得生命主体性；“抖落一串细碎的光”中，“抖落”二字力透纸背，将麻雀振翅的瞬时动能转化为可量度的光之碎屑，堪称动词炼金术。\n\n✅ 亮点：全篇无一处直写“温暖”“喜悦”，却通过“衣袖轻了”“呼吸深了”“心舒展如叶脉”等身体感知，让情绪具身化，深得沈从文式“以形写神”真传。\n\n⚠️ 可商榷处：“荠菜花…铺开一片清亮的碎银”——“碎银”意象稍近熟套（古诗常见“碎玉”“碎琼”）。若求陌生化突破，或可试：  \n> *“…在微光里浮起一层浮动的、薄薄的青霜。”*  \n（理由：“青霜”既存视觉清冷感，又暗含生机之凛冽，且与后文“泥土松软温润”构成温差张力）\n\n---\n\n**三、结构：环形复调，静默即高潮**  \n全文以风起始，以风收束（“quietly”），形成气韵闭环；中间三段由远（柳枝/桃李）及近（泥土/豌豆），再聚焦至极微（露珠/蛛网），最终落于人的身体觉知——这是典型的东方“由物及心”观照路径。最精妙的是破折号后的作家手记：它不解释文本，而成为文本的“元回声”，使短文获得自我指涉的现代性。\n\n✅ 亮点：英文译文精准复现了这一结构智慧。如将“欲言又止的问候”译为 *“an unspoken greeting”*，比直译“a greeting about to be spoken”更凝练；*“shyly unfurling two tender green hands”* 中“shyly”与原文“怯生生”神髓相契，且“unfurling”比“pushing out”更富生长张力。\n\n⚠️ 英译微瑕：*“scattering shards of light”* 美则美矣，但“shards”（碎片）隐含锋利感，稍损春日柔光本意。建议微调为：  \n> *“…scattering motes of light like startled pollen.”*  \n（理由：“motes”呼应尘埃微粒的轻盈悬浮感，“startled pollen”既延续“扑棱棱”的惊动感，又暗扣春日花粉飘散的生命隐喻，且与中文“抖落”形成跨语义通感）\n\n---\n\n**四、跨语转化：一次成功的“创造性叛逆”**  \n译文整体堪称典范：拒绝字字对应，专注气韵移植。如将“把世界重新翻译成柔软、希望与可生长的模样”处理为 *“rewrites the world into softness, hope, and the quiet certainty of growth”* ——“rewrites”替代“translates”，更契合“语法”隐喻；“quiet certainty”比直译“growable form”更具存在主义重量。唯一可斟酌处是末句 *“grammar reborn”*：虽准确，但英语读者或难瞬间领会其哲学分量。若面向国际文学期刊，可加半行脚注式点睛：  \n> *— Author’s Note: Spring is not merely a season—it is grammar reborn: the first syntax life speaks after winter’s silence.*\n\n---\n\n【结语：为何这篇短文值得被记住？】  \n《春信》证明：最深的春意，不在繁花盛景，而在人俯身时与泥土的0.5秒对视；最好的写作，不是描摹春天，而是让语言本身成为一株正在抽枝的植物——茎脉里奔涌汁液，叶缘上凝结露光。它不提供答案，只邀请重读：读风，读土，读自己突然变轻的袖口。\n\n如需，我可为您：  \n🔹 将此文延展为一组“二十四节气手记”系列  \n🔹 改写为儿童视角的童话诗体（保留所有自然细节，仅转换语态）  \n🔹 制作双语对照版，标注关键修辞的翻译决策逻辑  \n请随时吩咐——真正的评论，始于对文字的敬畏，终于对作者的成全。\n\n（签名）  \n林砚之｜《纸上的季节》专栏主笔  \n2024年春分·于檐角新燕筑巢处"},"messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"帮我写一篇关于春天的短文，然后翻译成英文"},{"messageType":"USER","metadata":{"messageType":"USER"},"rendered":false,"text":"你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"c6222819-ea48-9c0f-8cbd-d21a5d1b43cf","reasoningContent":""},"toolCalls":[],"media":[],"text":"当然可以。以下是我为您创作的一篇兼具诗意与温度的春天短文，文字凝练而富有画面感，力求在细微处见生机，在静默中听春声——\n\n**《春信》**  \n冬的余韵尚未散尽，风已悄然换了脾性：不再刺骨，只轻轻拂过耳际，像一句欲言又止的问候。柳枝最先接收到这密语，枯褐的枝条上，忽然鼓出点点青绒，仿佛大地在睡梦中微微睁开了眼。  \n\n桃李不争先后，各自酝酿着粉白的宣言；荠菜花则悄悄漫过田埂，在微光里铺开一片清亮的碎银。泥土松软温润，蚯蚓在暗处翻身，新翻的菜畦边，几粒豌豆正顶开薄土，怯生生地探出两片嫩绿的小手。  \n\n最是那清晨——露珠悬在蛛网上，晶莹得能照见整个微缩的春天；麻雀在刚抽芽的香椿树间扑棱棱飞起，抖落一串细碎的光。人站在院中，忽觉衣袖轻了，呼吸深了，心也跟着舒展如初展的叶脉。原来春天从不喧哗登场，它只是 quietly，把世界重新翻译成柔软、希望与可生长的模样。\n\n—— 作家手记：春非季节之名，乃万物重启的语法。\n\n---\n\n**English Translation: “The First Message of Spring”**  \nWinter’s echo has barely faded when the wind, too, changes its temper—no longer biting, but brushing softly past the ear like an unspoken greeting. The willow branches are the first to receive this quiet dispatch: tiny buds of downy green swell along their once-bare twigs, as if the earth itself were gently opening its eyes in sleep.  \n\nPeach and plum trees make no haste, each quietly preparing its rosy-white proclamation; meanwhile, shepherd’s-purse flowers slip unassumingly over field edges, spilling a shimmering scatter of silver light. The soil grows soft and warm. Beneath its surface, earthworms turn in the dark; beside freshly turned vegetable beds, pea seeds push through the thin crust of earth, shyly unfurling two tender green hands.  \n\nEspecially at dawn—the dewdrops hang suspended on spiderwebs, so clear they hold miniature springs within their curves; sparrows burst from newly leafing Chinese toon trees, scattering shards of light with every flutter of their wings. Standing in the courtyard, one suddenly feels the sleeves lighter, the breath deeper, the heart unfolding like a newly emerged leaf vein. For spring never arrives with fanfare—it simply arrives *quietly*, and rewrites the world into softness, hope, and the quiet certainty of growth.  \n\n— *Author’s Note: Spring is not merely a season—it is grammar reborn, the syntax by which all life begins again.*  \n\n如您有特定风格偏好（如更古典、童趣、哲思或地域特色），我很乐意为您重写或延展。愿这个春天，既有文字的微光，也有您窗外真实的风与花。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"ff666980-4399-9aab-bd5f-9b7fbdda782a","reasoningContent":""},"toolCalls":[],"media":[],"text":"[\"reviewer_agent\"]"},{"messageType":"USER","metadata":{"messageType":"USER"},"rendered":false,"text":"你是一个知名的评论家，擅长对文章进行评论和修改。待评审文章：{article_content}。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"08062b47-315a-9649-a3cc-ce3f9c0a2a47","reasoningContent":""},"toolCalls":[],"media":[],"text":"【评论家审读意见】  \n——评《春信》及其英译：一首以通感为笔、以节制为韵的微型春之赋格  \n\n作为一篇不足六百字的短章，《春信》远不止是应景小品，而是一次高度自觉的文学实践：它用汉语的肌理呼吸春天，又以翻译的镜面反照汉语的不可译性与可再生性。以下从立意、语言、结构与跨语转化四个维度作专业评述，并附精修建议（非“修改”，而是提供可供作者抉择的另一种可能）：\n\n---\n\n**一、立意：在“微物”中锚定存在诗学**  \n文章摒弃宏大抒情，将春天还原为可触、可听、可辨温的微观现场——“蚯蚓翻身”“豌豆探手”“蛛网悬露”，皆非修辞点缀，而是现象学式的凝视。尤为可贵的是结尾升维：“春非季节之名，乃万物重启的语法”，将自然律动提升至存在论层面，暗合海德格尔“语言是存在之家”的哲思，却全无术语堆砌，举重若轻。此为当代汉语散文稀缺的智性厚度。\n\n✅ 亮点：以“语法”喻春，既承袭古典“生生之谓易”的宇宙观，又赋予其现代语言哲学的锐度。\n\n⚠️ 建议微调：作家手记中“语法”一词稍显抽象。若追求更富张力的收束，可试作——  \n> *“春非四时之一，乃世界向自身发出的第一声元音。”*  \n（理由：“元音”兼具语言学本源性、发声的初生感与汉语单音节的韵律美，且与前文“欲言又止的问候”“密语”“宣言”形成声韵闭环）\n\n---\n\n**二、语言：汉语的“留白”与“触觉”双重胜利**  \n作者深谙汉语的未完成性美学：“怯生生地探出两片嫩绿的小手”——“怯生生”是拟人，“小手”是通感，“探出”是动态留白，三重叠加，使植物获得生命主体性；“抖落一串细碎的光”中，“抖落”二字力透纸背，将麻雀振翅的瞬时动能转化为可量度的光之碎屑，堪称动词炼金术。\n\n✅ 亮点：全篇无一处直写“温暖”“喜悦”，却通过“衣袖轻了”“呼吸深了”“心舒展如叶脉”等身体感知，让情绪具身化，深得沈从文式“以形写神”真传。\n\n⚠️ 可商榷处：“荠菜花…铺开一片清亮的碎银”——“碎银”意象稍近熟套（古诗常见“碎玉”“碎琼”）。若求陌生化突破，或可试：  \n> *“…在微光里浮起一层浮动的、薄薄的青霜。”*  \n（理由：“青霜”既存视觉清冷感，又暗含生机之凛冽，且与后文“泥土松软温润”构成温差张力）\n\n---\n\n**三、结构：环形复调，静默即高潮**  \n全文以风起始，以风收束（“quietly”），形成气韵闭环；中间三段由远（柳枝/桃李）及近（泥土/豌豆），再聚焦至极微（露珠/蛛网），最终落于人的身体觉知——这是典型的东方“由物及心”观照路径。最精妙的是破折号后的作家手记：它不解释文本，而成为文本的“元回声”，使短文获得自我指涉的现代性。\n\n✅ 亮点：英文译文精准复现了这一结构智慧。如将“欲言又止的问候”译为 *“an unspoken greeting”*，比直译“a greeting about to be spoken”更凝练；*“shyly unfurling two tender green hands”* 中“shyly”与原文“怯生生”神髓相契，且“unfurling”比“pushing out”更富生长张力。\n\n⚠️ 英译微瑕：*“scattering shards of light”* 美则美矣，但“shards”（碎片）隐含锋利感，稍损春日柔光本意。建议微调为：  \n> *“…scattering motes of light like startled pollen.”*  \n（理由：“motes”呼应尘埃微粒的轻盈悬浮感，“startled pollen”既延续“扑棱棱”的惊动感，又暗扣春日花粉飘散的生命隐喻，且与中文“抖落”形成跨语义通感）\n\n---\n\n**四、跨语转化：一次成功的“创造性叛逆”**  \n译文整体堪称典范：拒绝字字对应，专注气韵移植。如将“把世界重新翻译成柔软、希望与可生长的模样”处理为 *“rewrites the world into softness, hope, and the quiet certainty of growth”* ——“rewrites”替代“translates”，更契合“语法”隐喻；“quiet certainty”比直译“growable form”更具存在主义重量。唯一可斟酌处是末句 *“grammar reborn”*：虽准确，但英语读者或难瞬间领会其哲学分量。若面向国际文学期刊，可加半行脚注式点睛：  \n> *— Author’s Note: Spring is not merely a season—it is grammar reborn: the first syntax life speaks after winter’s silence.*\n\n---\n\n【结语：为何这篇短文值得被记住？】  \n《春信》证明：最深的春意，不在繁花盛景，而在人俯身时与泥土的0.5秒对视；最好的写作，不是描摹春天，而是让语言本身成为一株正在抽枝的植物——茎脉里奔涌汁液，叶缘上凝结露光。它不提供答案，只邀请重读：读风，读土，读自己突然变轻的袖口。\n\n如需，我可为您：  \n🔹 将此文延展为一组“二十四节气手记”系列  \n🔹 改写为儿童视角的童话诗体（保留所有自然细节，仅转换语态）  \n🔹 制作双语对照版，标注关键修辞的翻译决策逻辑  \n请随时吩咐——真正的评论，始于对文字的敬畏，终于对作者的成全。\n\n（签名）  \n林砚之｜《纸上的季节》专栏主笔  \n2024年春分·于檐角新燕筑巢处"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"21e211c1-9994-9bea-9ee5-29aee332ccd1","reasoningContent":""},"toolCalls":[],"media":[],"text":"[]"}],"supervisor_decision":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"21e211c1-9994-9bea-9ee5-29aee332ccd1","reasoningContent":""},"toolCalls":[],"media":[],"text":"[]"}}}}
         * 17:46:55.629 [default-executor-shutdown-hook] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Shutting down ParallelNode default executor
         * 17:46:55.629 [default-executor-shutdown-hook] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- ParallelNode default thread pool terminated
         * 17:46:55.629 [default-executor-shutdown-hook] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- ParallelNode default executor shut down successfully
         *
         */


    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        return chatModel;
    }
}
