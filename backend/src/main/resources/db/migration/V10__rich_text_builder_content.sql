alter table test_sections add column instructions_document text;
alter table test_sections add column instructions_text text;
alter table test_sections add column instructions_rendered_html text;
alter table test_sections add column passage_document text;
alter table test_sections add column passage_text_rendered_html text;

alter table reading_paragraphs add column content_document text;
alter table reading_paragraphs add column content_rendered_html text;

alter table questions add column question_document text;
alter table questions add column question_rendered_html text;
alter table questions add column instructions_document text;
alter table questions add column instructions_text text;
alter table questions add column instructions_rendered_html text;
