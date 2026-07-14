alter table question_options add column option_document text;
alter table question_options add column option_rendered_html text;

alter table question_answers add column explanation_document text;
alter table question_answers add column explanation_rendered_html text;
